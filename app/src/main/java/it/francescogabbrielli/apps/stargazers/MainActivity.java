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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.android.AndroidInjection;
import it.francescogabbrielli.apps.stargazers.controller.EndlessRecyclerViewScrollListener;
import it.francescogabbrielli.apps.stargazers.controller.WebViewClient;
import it.francescogabbrielli.apps.stargazers.model.GitHubService;
import it.francescogabbrielli.apps.stargazers.model.GitHubUser;
import it.francescogabbrielli.apps.stargazers.model.RecyclerAdapter;
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
 * Search and display all of GitHub stargazers!
 *
 * REFERENCES
 * ==========
 * https://developer.android.com/tr aining/appbar/setting-up.html#java
 * https://developer.android.com/training/search/setup.html?#create-sc
 * https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
 * https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView
 * https://futurestud.io/tutorials/retrofit-2-activate-response-caching-etag-last-modified
 */
public class MainActivity extends AppCompatActivity
        implements
        MenuItem.OnActionExpandListener,
        SearchView.OnQueryTextListener,
        Callback<List<GitHubUser>> {

    private final static String TAG = "Stargazers-Main";

    private final static String KEY_REPO_OWNER      = "owner";
    private final static String KEY_REPO_NAME       = "repo";
    private final static String KEY_QUERY           = "query";
    private final static String KEY_FOCUS           = "focus";
    private final static String KEY_LAST_PAGE       = "last_page";

    private RecyclerView recycler;
    private RecyclerAdapter adapter;

    private MenuItem searchItem;
    private SearchView searchView;

    private EndlessRecyclerViewScrollListener scrollListener;

    @Inject @Named("main")
    GitHubService service;

    private CharSequence searchQuery;
    private boolean searchFocus;

    private String repoOwner, repoName;
    private int lastPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        setupRecycler();
//        setupRetrofit();

        if (savedInstanceState!=null) {
            adapter.readFromBundle(savedInstanceState);
            scrollListener.retrieveState(savedInstanceState);
            repoOwner = savedInstanceState.getString(KEY_REPO_OWNER);
            repoName = savedInstanceState.getString(KEY_REPO_NAME);
            searchQuery = savedInstanceState.getCharSequence(KEY_QUERY);
            searchFocus = savedInstanceState.getBoolean(KEY_FOCUS);
            lastPage = savedInstanceState.getInt(KEY_LAST_PAGE);
        }

        setupTitleAndHome();
    }

    //----------------------------------------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="STATE METHODS: These methods determine the state of the activity">
    //
    // STATE METHODS: These methods determine the state of the activity
    //----------------------------------------------------------------------------------------------
    //
    private boolean isRepoSet() {
        return repoName!=null;
    }

    private boolean isError() {
        return adapter.getStatus()==RecyclerAdapter.STATUS_ERROR;
    }

    private void setError(@NonNull Throwable t) {
        String errMsg = getString(R.string.error_response, t.getMessage());
        Log.e(TAG, "Page "+scrollListener.getCurrentPage()+ " - " + errMsg, t);
        setError(errMsg);
    }

    private void setError(String errMsg) {
        Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();

        //if is already scrolling don't set the error state...
        //...just leave the scrolling loading going back to the previous page
        if (scrollListener.getCurrentPage()<=1) {
            repoName = null;
            adapter.setError(errMsg);
        } else {
            scrollListener.fail();
        }
    }

    private boolean isOk() {
        return adapter.hasUsers();
    }

    private boolean isSearchFocused() {
        return searchFocus;
    }

    private boolean isQueryTyped() {
        return !TextUtils.isEmpty(searchQuery);
    }
    //
    // </editor-fold>
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="SETUP METHODS">
    //
    // SETUP METHODS
    //----------------------------------------------------------------------------------------------
    //
    private void setupTitleAndHome() {

        //setup title
        CharSequence title = getString(R.string.welcome);
        if (isRepoSet())
            title = repoOwner + "/" + repoName;
        setTitle(title);

        //update webview with homepage
        WebView v = findViewById(R.id.homepage);
        if (isRepoSet() || isOk() || isError())
            v.setVisibility(View.GONE);
        else if (v.getUrl()==null)
            new WebViewClient(this, v)
                    .load(getString(R.string.home_page));

    }

    private void setupRecycler() {
        recycler = findViewById(R.id.list_results);
        recycler.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.v(TAG, String.format("Try loading more data for page %d, having %d items", page, totalItemsCount));
                if (totalItemsCount > GitHubService.PER_PAGE)
                    loadNextDataFromApi(page);
                else
                    resetState();
            }
        };
        adapter = new RecyclerAdapter(this, scrollListener);
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(scrollListener);
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
    //
    // SETUP METHODS
    // </editor-fold>
    //----------------------------------------------------------------------------------------------

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchManager==null) {
            Toast.makeText(this,
                    R.string.error_search_service_not_found,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        if (isSearchFocused() && isQueryTyped()) {
            new Handler().post(() -> {
                searchView.setIconified(false);
                searchItem.expandActionView();
                searchView.setQuery(searchQuery, false);
                searchItem.setOnActionExpandListener(this);
                searchView.setOnQueryTextListener(this);
            });
        } else {
            searchItem.setOnActionExpandListener(this);
            searchView.setOnQueryTextListener(this);
        }

        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        WebView v = findViewById(R.id.homepage);
        if (!isOk() || !isQueryTyped()) {
            adapter.resetStatus();
            repoName = null;
            v.setVisibility(View.VISIBLE);
            setupTitleAndHome();
        }
        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (isRepoSet()) {
            searchView.post(() -> {
                searchView.setOnQueryTextListener(null);
                searchQuery = repoOwner + "/" + repoName;
                searchView.setQuery(searchQuery, false);
                searchView.setOnQueryTextListener(this);
            });
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (!TextUtils.equals(newText, searchQuery))
            searchQuery = newText;
        searchFocus = true;
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putString(KEY_REPO_OWNER, repoOwner);
        outState.putString(KEY_REPO_NAME, repoName);

        adapter.writeToBundle(outState);
        scrollListener.saveState(outState);

        // update searchQuery and searchFocus only from the search field (if it has layout)
        searchQuery = null;
        searchFocus = false;
        if (searchView!=null) {
            searchQuery = searchView.getQuery();
            searchFocus = !searchView.isIconified();
        }

        outState.putCharSequence(KEY_QUERY, searchQuery);
        outState.putBoolean(KEY_FOCUS, searchFocus);
        outState.putInt(KEY_LAST_PAGE, lastPage);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        Log.v(TAG, "Action: "+intent.getAction());

        // search stargazers
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "Query: " + query);
            handleSearch(query);

        // handle suggestion click or direct user search
        } else if(Intent.ACTION_VIEW.equals(intent.getAction())) {

            String query = intent.getDataString();
            if (query==null)
                return;
            if (!query.contains("/")) {
                if (searchView.isIconified()) {
                    searchItem.setOnActionExpandListener(null);
//                    new MenuItem.OnActionExpandListener() {
//                        @Override
//                        public boolean onMenuItemActionExpand(MenuItem item) {
//                            return true;
//                        }
//
//                        @Override
//                        public boolean onMenuItemActionCollapse(MenuItem item) {
//                            return true;
//                        }
//                    });
                    searchView.setIconified(false);
                    searchItem.expandActionView();
                    searchItem.setOnActionExpandListener(this);
                }
                searchView.setQuery(query+"/", false);
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            } else {
                searchView.setQuery(query, true);
            }

        }
    }

    private void handleSearch(String query) {
        try {
            String[] parts = query.split("/");
            repoOwner = parts[0];
            repoName = parts[1];
            adapter.setLoading();
            lastPage = 0;
            Log.d(TAG, "Loading first page for " + repoName);
            service.listStargazers(repoOwner, repoName, 1).enqueue(this);
        } catch (Exception e) {
            adapter.setError(getString(R.string.error_query, query));
            Log.e(TAG, getString(R.string.error_query, query), e);
        } finally {
            setupTitleAndHome();
        }
    }

    // NB: is safe to store pages in local variables with no synchronization
    //     because consecutive loading cannot overlap as the implicit implementation
    //     of the ScrollListener does not allow a new call until the previous returned
    //     some data or failed
    private void loadNextDataFromApi(int page) {
        if (page<=lastPage) {
//            currentPage = page;
            Log.d(TAG, "Loading page " + page);
            service.listStargazers(repoOwner, repoName, page).enqueue(this);
        }
    }

    @Override
    public void onResponse(@NonNull Call<List<GitHubUser>> call,
                           @NonNull Response<List<GitHubUser>> response) {

        try {

            if (response.isSuccessful()) {

                Headers headers = response.headers();
                List<GitHubUser> users = response.body();

                // count items and pages only the first time of page loading
                if (adapter.getItemCount()<GitHubService.PER_PAGE) {
                    lastPage = GitHubService.findLastPage(headers);
                    count(lastPage, users.size());
                }

                adapter.addData(users, scrollListener.getCurrentPage() < lastPage);

                // collapse search bar
                searchView.post(() -> {
                    searchView.setOnQueryTextListener(null);
                    searchItem.setOnActionExpandListener(null);
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                    searchItem.collapseActionView();
                    searchItem.setOnActionExpandListener(this);
                    searchView.setOnQueryTextListener(this);
                });

            } else {

                setError(response.message());

            }

        } catch(Exception e) {

            setError(e);

        } finally {

            setupTitleAndHome();

        }
    }

    @Override
    public void onFailure(@NonNull Call<List<GitHubUser>> call, @NonNull Throwable t) {
        setError(t);
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
            service.listStargazers(repoOwner, repoName, lastPage).enqueue(
                    new Callback<List<GitHubUser>>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<List<GitHubUser>> call,
                                @NonNull Response<List<GitHubUser>> response) {
                            if (response.isSuccessful())
                                showCount(GitHubService
                                        .countUsers(lastPage, response.body()));
                            //TODO: else show a message
                        }
                        @Override
                        public void onFailure(
                                @NonNull Call<List<GitHubUser>> call,
                                @NonNull Throwable t) {
                            //TODO: show a message
                        }
                    });
    }

    private void showCount(int n) {
        Toast.makeText(
                this,
                getString(R.string.search_count, n),
                Toast.LENGTH_LONG).show();
    }

}
