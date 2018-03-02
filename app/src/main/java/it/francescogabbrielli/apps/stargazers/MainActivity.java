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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private final static String KEY_LAST            = "last";

    private RecyclerView recycler;
    private RecyclerAdapter adapter;

    private MenuItem searchItem;
    private SearchView searchView;

    private EndlessRecyclerViewScrollListener scrollListener;

    private GitHubService service;

    private CharSequence searchQuery;
    private boolean searchFocus;

    private String repoOwner, repoName;
    private int currentPage, lastPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        setupRecycler();
        setupRetrofit();

        if (savedInstanceState!=null) {
            adapter.readFromBundle(savedInstanceState);
            repoOwner = savedInstanceState.getString(KEY_REPO_OWNER);
            repoName = savedInstanceState.getString(KEY_REPO_NAME);
            searchQuery = savedInstanceState.getCharSequence(KEY_QUERY);
            searchFocus = savedInstanceState.getBoolean(KEY_FOCUS);
            lastPage = savedInstanceState.getInt(KEY_LAST);
        }

        setupTitleAndHome();
    }

    //----------------------------------------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="STATE METHODS: These methods determine the state of the activity">
    //
    // STATE METHODS: These methods determine the state of the activity
    //----------------------------------------------------------------------------------------------
    //
    //----------------------------------------------------------------------------------------------
    //
    //----------------------------------------------------------------------------------------------
    //
    private boolean isRepoSet() {
        return repoName!=null;
    }

    private boolean isError() {
        return adapter.getStatus()==RecyclerAdapter.STATUS_ERROR;
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
    // </editor-fold>
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // <editor-fold defaultstate="collapsed" desc="SETUP METHODS">
    //
    // SETUP METHODS
    //----------------------------------------------------------------------------------------------
    //
    private void setupHomePage() {

    }

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

        else if (v.getUrl()==null) {
            v.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    //TODO: handle no connection
                }
            });
            v.loadUrl(getString(R.string.home_page));
        }

    }

    private void setupRecycler() {
        recycler = findViewById(R.id.list_results);
        recycler.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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
        if (isRepoSet())
            searchView.post(()-> {
                searchView.setOnQueryTextListener(null);
                searchQuery = repoOwner+"/"+repoName;
                searchView.setQuery(searchQuery, false);
                searchView.setOnQueryTextListener(this);
            });
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
//        searchQuery = query;
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putString(KEY_REPO_OWNER, repoOwner);
        outState.putString(KEY_REPO_NAME, repoName);

        adapter.writeToBundle(outState);

        // update searchQuery and searchFocus only from the search field (if it has layout)
        searchQuery = null;
        searchFocus = false;
        if (searchView!=null) {
            searchQuery = searchView.getQuery();
            searchFocus = !searchView.isIconified();
        }

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
            Log.d(TAG, "Query: " + query);
            handleSearch(query);

        // handle suggestion click
        } else if(Intent.ACTION_VIEW.equals(intent.getAction())) {

            String query = intent.getDataString();
            if (query==null)
                return;
            if (!query.contains("/")) {
                if (searchView.isIconified()) {
                    searchView.setIconified(false);
                    searchItem.expandActionView();
                }
                searchView.setQuery(query+"/", false);
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            } else {
                handleSearch(query);
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
            currentPage = page;
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
                    currentPage = 1;
                    lastPage = GitHubService.findLastPage(headers);
                    count(lastPage, users.size());
                }

                adapter.addData(users, currentPage < lastPage);

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

                repoName = null;
                adapter.setError(response.message());

            }

        } catch(Exception e) {

            //This should not happen ...
            repoName = null;
            String errMsg = String.valueOf(getString(R.string.error_response, e.getMessage()));
            adapter.setError(errMsg);
            failScrolling();
            Log.e(TAG, getString(R.string.error_response, errMsg), e);

        } finally {

            setupTitleAndHome();

        }
    }

    @Override
    public void onFailure(@NonNull Call<List<GitHubUser>> call, @NonNull Throwable t) {

        String errMsg = getString(R.string.error_loading, String.valueOf(t.getMessage()));
        if (currentPage<=1)
            adapter.setError(errMsg);

        else
            failScrolling();

        Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Pag "+currentPage+" - "+t, t);
    }

    private void failScrolling() {
        recycler.post(() -> {
            currentPage--;
            scrollListener.fallback();
        });
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
                        }
                        @Override
                        public void onFailure(
                                @NonNull Call<List<GitHubUser>> call,
                                @NonNull Throwable t) {}
                    });
    }

    private void showCount(int n) {
        Toast.makeText(
                this,
                getString(R.string.search_count, n),
                Toast.LENGTH_LONG).show();
    }

}
