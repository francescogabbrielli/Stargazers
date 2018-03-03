package it.francescogabbrielli.apps.stargazers;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Francesco Gabbrielli on 21/02/18.
 *
 * REFERENCES
 * ==========
 * https://developer.android.com/guide/topics/providers/content-provider-creating.html
 * https://developer.android.com/guide/topics/search/adding-custom-suggestions.html
 */
public class SuggestionProvider extends ContentProvider {

    public final static String TAG = "Stargazers-Provider";

    // Creates a UriMatcher object.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(SuggestionProvider.class.getCanonicalName(), "search_suggest_query", 1);
        sUriMatcher.addURI(SuggestionProvider.class.getCanonicalName(), "search_suggest_query#", 2);
    }

    private GitHubService service;

    @Override
    public boolean onCreate() {

        Cache cache = new Cache(getContext().getCacheDir(), R.integer.cache_size);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        int timeout = getContext().getResources().getInteger(R.integer.hint_timeout);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .cache(cache)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getContext().getString(R.string.github_api_baseurl))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        service = retrofit.create(GitHubService.class);

        return true;

    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return query(uri,projection, selection, selectionArgs, sortOrder, null);
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder,
            @Nullable CancellationSignal signal) {

        MatrixCursor ret = new MatrixCursor(new String[] {
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA
        }, 0);

        switch (sUriMatcher.match(uri)) {
            case 1:
                if (selectionArgs!=null && selectionArgs.length>0)
                    apiQuery(ret, selectionArgs[0]);
                break;
            default:
                Log.w(TAG, "Unhandled query: "+sUriMatcher.match(uri));
        }
        return ret;
    }

    private void apiQuery(MatrixCursor cursor, String query) {

        try {

            Thread.sleep(200);//TODO: implement debouncing instead

            int hintsNr = getContext().getResources().getInteger(R.integer.hint_max_nr);

            if (query.contains("/")) {

                String[] parts = query.split("/");
                String repoQuery = parts.length>1 ? parts[1] : "";

                Response<GitHubSearch<GitHubRepo>> response =
                        service.searchRepos(repoQuery+"+user:"+parts[0], "stars", hintsNr)
                                .execute();

                Log.v(TAG, "Query "+parts[0]+"'s repos: "+repoQuery+"+user:"+parts[0]);

                int i = 0;
                if (response.isSuccessful())
                    for (GitHubRepo r : response.body().getItems()) {
                        if (query.equals(r.getFullName()))
                            break;
                        cursor.addRow(new Object[]{++i, r.getName(), r.getFullName()});
                        Log.v(TAG, "- "+r.getFullName());
                    }

            } else if (!query.isEmpty()){

                Response<GitHubSearch<GitHubUser>> response =
                        service.searchUsers(query+"+in:login+in:fullname", hintsNr)
                                .execute();

                Log.v(TAG, "Query users: "+query);

                int i = 0;
                if (response.isSuccessful())
                    for (GitHubUser u : response.body().getItems()) {
                        cursor.addRow(new Object[]{++i, u.getLogin(), u.getLogin()});
                        Log.v(TAG, "- " + u.getLogin());
                    }

            }

        } catch(Exception e) {
            //Toast.makeText(getContext(), R.string.error_loading, Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage(), e);
        }

    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

}
