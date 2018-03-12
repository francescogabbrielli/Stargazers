package it.francescogabbrielli.apps.stargazers;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.ActivityKey;
import dagger.android.AndroidInjector;
import dagger.android.ContentProviderKey;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasContentProviderInjector;
import dagger.multibindings.IntoMap;

import it.francescogabbrielli.apps.stargazers.model.GitHubService;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Francesco Gabbrielli on 5/03/18.
 *
 * REFERENCES
 * ==========
 * https://google.github.io/dagger/android
 * https://stackoverflow.com/questions/9873669/how-do-i-catch-content-provider-initialize
 */
public class StargazersApp extends Application
        implements HasActivityInjector, HasContentProviderInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingActivityInjector;

    @Inject
    DispatchingAndroidInjector<ContentProvider> dispatchingProviderInjector;

    @Subcomponent
    interface MainActivitySubcomponent extends AndroidInjector<MainActivity> {
        @Subcomponent.Builder
        abstract class Builder extends AndroidInjector.Builder<MainActivity> {}
    }

    @Subcomponent
    interface SuggestionProviderSubcomponent extends AndroidInjector<SuggestionProvider> {
        @Subcomponent.Builder
        abstract class Builder extends AndroidInjector.Builder<SuggestionProvider> {}
    }

    @Module(includes = RetrofitModule.class, subcomponents = {MainActivitySubcomponent.class, SuggestionProviderSubcomponent.class})
    abstract class MainModule {

        @Binds
        @IntoMap
        @ActivityKey(MainActivity.class)
        abstract AndroidInjector.Factory<? extends Activity>
        bindMainActivityInjectorFactory(MainActivitySubcomponent.Builder builder);

        @Binds
        @IntoMap
        @ContentProviderKey(SuggestionProvider.class)
        abstract AndroidInjector.Factory<? extends ContentProvider>
        bindSuggestionProviderInjectorFactory(SuggestionProviderSubcomponent.Builder builder);

    }

    @Scope
    @interface ActivityScope {}

    @Scope
    @interface ProviderScope {}

    @Module
    class RetrofitModule {

        @Provides
        OkHttpClient.Builder provideHttpClientBuilder() {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            Cache cache = new Cache(getCacheDir(), R.integer.cache_size);
            return new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .cache(cache);
        }

        @Provides
        @Singleton
        @Named("main")
        Retrofit provideRetrofitMain(OkHttpClient.Builder okHttpClientBuilder) {
            Log.d("Stargazers", "Main Retrofit created");
            int timeout = getResources().getInteger(R.integer.timeout);
            return buildRetrofit(okHttpClientBuilder
                    .connectTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .build());
        }

        @Provides
        @Singleton
        @Named("hint")
        Retrofit provideRetrofitHint(OkHttpClient.Builder okHttpClientBuilder) {
            Log.d("Stargazers", "Hint Retrofit created");
            int timeout = getResources().getInteger(R.integer.hint_timeout);
            return buildRetrofit(okHttpClientBuilder
                    .connectTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .build());
        }

        private Retrofit buildRetrofit(OkHttpClient client) {
            return new Retrofit.Builder()
                    .baseUrl(getString(R.string.github_api_baseurl))
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        @Provides
        @Singleton
        @Named("main")
        GitHubService provideGitHubServiceMain(@Named("main") Retrofit retrofit) {
            return retrofit.create(GitHubService.class);
        }

        @Provides
        @Singleton
        @Named("hint")
        GitHubService provideGitHubServiceHint(@Named("hint") Retrofit retrofit) {
            return retrofit.create(GitHubService.class);
        }

    }

    @Singleton
    @Component(modules = MainModule.class)
    interface ApplicationComponent {
        void inject(StargazersApp app);
    }

    @Override
    //use this instead of onCreate to build injection
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        DaggerStargazersApp_ApplicationComponent.builder()
                .retrofitModule(new RetrofitModule())
                .build()
                .inject(this);
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingActivityInjector;
    }

    @Override
    public AndroidInjector<ContentProvider> contentProviderInjector() {
        return dispatchingProviderInjector;
    }

}
