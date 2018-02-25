package it.francescogabbrielli.apps.stargazers;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * See all GitHub stargazers!
 *
 * REFERENCES
 * ==========
 * https://developer.android.com/tr aining/appbar/setting-up.html#java
 * https://developer.android.com/training/search/setup.html?#create-sc
 * https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
 * https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView
 * https://futurestud.io/tutorials/retrofit-2-activate-response-caching-etag-last-modified
 */
public class MainActivity extends AppCompatActivity implements Callback<List<GitHubUser>> {

    public final static String TAG = "Stargazers";

    private final static String KEY_REPO_OWNER      = "owner";
    private final static String KEY_REPO_NAME       = "repo";
    private final static String KEY_QUERY           = "query";
    private final static String KEY_FOCUS           = "focus";
    private final static String KEY_LAST            = "last";


    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;
    private RecyclerAdapter adapter;

    private MenuItem searchItem;
    private SearchView searchView;
    private boolean searchFocus;

    private EndlessRecyclerViewScrollListener scrollListener;

    private GitHubService service;

    private String repoOwner, repoName;
    private CharSequence searchQuery;

    private int currentPage, lastPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        setupRecyler();
        setupRetrofit();

        searchFocus = true;

        if (savedInstanceState!=null) {
            adapter.readFromBundle(savedInstanceState);
            repoOwner = savedInstanceState.getString(KEY_REPO_OWNER);
            repoName = savedInstanceState.getString(KEY_REPO_NAME);
            searchQuery = savedInstanceState.getCharSequence(KEY_QUERY);
            searchFocus = savedInstanceState.getBoolean(KEY_FOCUS);
            lastPage = savedInstanceState.getInt(KEY_LAST);
        }

        setTitle(makeTitle());
    }

    private CharSequence makeTitle() {
        CharSequence ret = getString(R.string.app_name);
        if (repoName!=null)
            ret = repoOwner + "/" + repoName;
        return ret;
    }

    private void setupRecyler() {
        recycler = findViewById(R.id.list_results);
        recycler.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.d(TAG, String.format("Try loading more data for page %d, having %d items", page, totalItemsCount));
                if (totalItemsCount > GitHubService.PER_PAGE)
                    loadNextDataFromApi(page);
                else
                    resetState();
            }
        };
        recycler.addOnScrollListener(scrollListener);
        adapter = new RecyclerAdapter(this, scrollListener);
        recycler.setAdapter(adapter);
    }

    private void setupRetrofit() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        Cache cache = new Cache(getCacheDir(), R.integer.cache_size);
        int timeout = getResources().getInteger(R.integer.timeout);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .cache(cache)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.github_api_baseurl))
                .client(okHttpClient)
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
        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        if (!TextUtils.isEmpty(searchQuery))
            new Handler().post(() -> {
//                searchView.setQuery("", false);
                searchView.setIconified(false);
                searchItem.expandActionView();
                searchView.setQuery(searchQuery, false);
            });

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putString(KEY_REPO_OWNER, repoOwner);
        outState.putString(KEY_REPO_NAME, repoName);

        adapter.writeToBundle(outState);

        searchQuery = searchView.getQuery();//Not needed?
        searchFocus = searchView.isFocused();//Not needed?
        outState.putCharSequence(KEY_QUERY, searchQuery);
        outState.putBoolean(KEY_FOCUS, searchFocus);
        outState.putInt(KEY_LAST, lastPage);

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

        // handle suggestion click
        } else if(Intent.ACTION_VIEW.equals(intent.getAction())) {

            String query = intent.getDataString();
            if (!query.contains("/")) {
                searchView.setQuery(query+"/", false);
            } else {
                handleSearch(query);
            }

        }
    }

    private void handleSearch(String query) {
        try {
//            adapter.reset();
            String[] parts = query.split("/");
            repoOwner = parts[0];
            repoName = parts[1];
            adapter.setLoading();
            service.listStargazers(repoOwner, repoName, 1).enqueue(this);
        } catch (Exception e) {
            adapter.setError(getString(R.string.error_query, query));
            Log.e(TAG, getString(R.string.error_query, query), e);
        }
    }

    private void loadNextDataFromApi(int page) {
        if (page<=lastPage) {
            currentPage = page;
            Log.d(TAG, "Loading page " + page);
            service.listStargazers(repoOwner, repoName, page).enqueue(this);
        }
    }

    @Override
    public void onResponse(Call<List<GitHubUser>> call, Response<List<GitHubUser>> response) {

        try {

            if (response.isSuccessful()) {

                Headers headers = response.headers();
                List<GitHubUser> users = response.body();

                // count only the first time
                if (adapter.getItemCount()<GitHubService.PER_PAGE) {
                    lastPage = GitHubService.findLastPage(headers);
                    count(lastPage, users.size());
                }

                adapter.addData(users, currentPage < lastPage);

                //collapse search bar
                searchView.post(() -> {
                    searchQuery = "";
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                    searchItem.collapseActionView();
                    setTitle(makeTitle());
                });

            } else

                adapter.setError(response.message());

        } catch(Exception e) {
            adapter.setError(e.getMessage());
            recycler.post(() -> scrollListener.fallback());
            Log.e(TAG, getString(R.string.error_response), e);
        }
    }

    @Override
    public void onFailure(Call<List<GitHubUser>> call, Throwable t) {
        adapter.setError(getString(R.string.error_loading));
        recycler.post(() -> scrollListener.fallback());
        Log.e(TAG, getString(R.string.error_loading), t);
    }

    /**
     * Count all users, eventually using an additional call to retrieve the last page.
     * The result is shown in a Toast
     *
     * @param lastPage
     *      the last page of the data
     * @param currentPageSize
     *      the size of the currently loaded page
     */
    private void count(final int lastPage, int currentPageSize) {
        if (lastPage<=1)
            showCount(currentPageSize);
        else
            service.listStargazers(repoOwner, repoName, lastPage).enqueue(new Callback<List<GitHubUser>>() {
                    @Override
                    public void onResponse(Call<List<GitHubUser>> call, Response<List<GitHubUser>> response) {
                        if (response.isSuccessful())
                            showCount(GitHubService.countUsers(lastPage, response.body()));
                    }
                    @Override
                    public void onFailure(Call<List<GitHubUser>> call, Throwable t) {}
                });
    }

    private void showCount(int n) {
        Toast.makeText(
                this,
                getString(R.string.search_count, n),
                Toast.LENGTH_LONG).show();
    }

}
