package com.ewintory.udacity.popularmovies.data;

import android.app.Application;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.ewintory.udacity.popularmovies.data.api.ApiModule;
import com.ewintory.udacity.popularmovies.data.model.Movie;
import com.ewintory.udacity.popularmovies.data.model.MovieStorIOSQLiteDeleteResolver;
import com.ewintory.udacity.popularmovies.data.model.MovieStorIOSQLiteGetResolver;
import com.ewintory.udacity.popularmovies.data.model.MovieStorIOSQLitePutResolver;
import com.ewintory.udacity.popularmovies.utils.MoviesGlideModule;
import com.ewintory.udacity.popularmovies.utils.MoviesHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.SECONDS;

@Module(
        includes = ApiModule.class,
        injects = {
                MoviesGlideModule.class
        },
        complete = false,
        library = true
)
public final class DataModule {
    public static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 100MB

//    @Provides @Singleton public StorIOSQLite provideStorIOSQLite(@NonNull SQLiteOpenHelper sqLiteOpenHelper) {
//        return DefaultStorIOSQLite.builder()
//                .sqliteOpenHelper(sqLiteOpenHelper)
//                .addTypeMapping(Movie.class, SQLiteTypeMapping.<Movie>builder()
//                        .putResolver(new MovieStorIOSQLitePutResolver())
//                        .getResolver(new MovieStorIOSQLiteGetResolver())
//                        .deleteResolver(new MovieStorIOSQLiteDeleteResolver())
//                        .build())
//                .build();
//    }


    @Provides @Singleton Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides @Singleton OkHttpClient provideOkHttpClient(Application app) {
        return createOkHttpClient(app);
    }

    @Provides @Singleton Picasso providePicasso(Application app, OkHttpClient client) {
        return new Picasso.Builder(app)
                .downloader(new OkHttpDownloader(client))
                .requestTransformer(new Picasso.RequestTransformer() {
                    @Override public Request transformRequest(Request request) {
                        String fullPath = MoviesHelper.buildPosterImageUrl(request.uri.getPath(), request.targetWidth);

                        Timber.v("Full path: " + fullPath);
                        return request.buildUpon().setUri(Uri.parse(fullPath)).build();
                    }
                })
                .listener(new Picasso.Listener() {
                    @Override public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
                        Timber.e(e, "Failed to load image: %s", uri);
                    }
                })
                .build();
    }

    static OkHttpClient createOkHttpClient(Application app) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(10, SECONDS);
        client.setReadTimeout(10, SECONDS);
        client.setWriteTimeout(10, SECONDS);

        // Install an HTTP cache in the application cache directory.
        File cacheDir = new File(app.getCacheDir(), "http");
        Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
        client.setCache(cache);

        return client;
    }
}
