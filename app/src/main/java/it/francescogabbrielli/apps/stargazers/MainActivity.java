package it.francescogabbrielli.apps.stargazers;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * See all GitHub stargazers
 *
 * REFERENCES
 * ==========
 * https://developer.android.com/tr aining/appbar/setting-up.html#java
 * https://developer.android.com/training/search/setup.html?#create-sc
 * https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
 * https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView
 */
public class MainActivity extends AppCompatActivity implements Callback<List<GitHubUser>> {

    private final static String TAG = "Stargazers";

    private final static String KEY_REPO_OWNER      = "owner";
    private final static String KEY_REPO_NAME       = "repo";

    private final static String HEADER_LINK         = "Link";
    private final static String HEADER_LINK_NEXT    = "rel=\"next\"";

    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;
    private RecyclerAdapter adapter;

    private SearchView searchView;

    private EndlessRecyclerViewScrollListener scrollListener;

    private GitHubService service;

    private String repoOwner, repoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupRecyler();
        setupRetrofit();

        if (savedInstanceState!=null) {
            adapter.readFromBundle(savedInstanceState);
            repoOwner = savedInstanceState.getString(KEY_REPO_OWNER);
            repoName = savedInstanceState.getString(KEY_REPO_NAME);
        }
    }

    private void setupRecyler() {
        recycler = findViewById(R.id.list_results);
        recycler.setHasFixedSize(true);
        adapter = new RecyclerAdapter(this);
        recycler.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(page+1);
            }
        };
        recycler.addOnScrollListener(scrollListener);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.github_api_baseurl))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GitHubService.class);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_REPO_OWNER, repoOwner);
        outState.putString(KEY_REPO_NAME, repoName);
        adapter.writeToBundle(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        // search stargazers
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "Query: "+query);
            handleSearch(query);

        } else if(Intent.ACTION_VIEW.equals(intent.getAction())) {

            String query = intent.getDataString();
            if (!query.contains("/")) {
                searchView.setQuery(query+"/", false);
            } else {
                searchView.setQuery(query, false);
                handleSearch(query);
            }

        }
    }

    private void handleSearch(String query) {
        try {
            adapter.reset();
            String[] parts = query.split("/");
            repoOwner = parts[0];
            repoName = parts[1];
            service.listStargazers(repoOwner, repoName, 1).enqueue(this);
        } catch (Exception e) {
            adapter.setError(getString(R.string.error_query, query));
            Log.e(TAG, getString(R.string.error_query, query), e);
        }
    }

    private void loadNextDataFromApi(int page) {
        if (adapter.hasMoreData()) {
            Log.d(TAG, "Loading page " + page);
            service.listStargazers(repoOwner, repoName, page).enqueue(this);
        }
    }

    @Override
    public void onResponse(Call<List<GitHubUser>> call, Response<List<GitHubUser>> response) {
        try {
            if (response.isSuccessful()) {
                String links = response.headers().get(HEADER_LINK);
                if (links == null || !links.contains(HEADER_LINK_NEXT))
                    adapter.setNoMoreData(true);
                adapter.addData(response.body());
            } else
                adapter.setError(response.message());
        } catch(Exception e) {
            adapter.setError(e.getMessage());
            recycler.postDelayed(() -> scrollListener.fallback(), 1000);
            Log.e(TAG, getString(R.string.error_response), e);
        }
    }

    @Override
    public void onFailure(Call<List<GitHubUser>> call, Throwable t) {
        recycler.postDelayed(() -> scrollListener.fallback(), 1000);
        Log.e(TAG, getString(R.string.error_loading), t);
    }

}
