package org.wikipedia.dataclient.okhttp;

import android.net.Uri;
import android.os.SystemClock;

import com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher;

import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class CacheableOkHttpNetworkFetcher extends OkHttpNetworkFetcher {
    public CacheableOkHttpNetworkFetcher(OkHttpClient okHttpClient) {
        super(okHttpClient);
    }

    public CacheableOkHttpNetworkFetcher(Call.Factory callFactory, Executor cancellationExecutor) {
        super(callFactory, cancellationExecutor);
    }

    @Override
    public void fetch(OkHttpNetworkFetchState fetchState, Callback callback) {
        // Identical to the super except that caching is not forbidden
        fetchState.submitTime = SystemClock.elapsedRealtime();
        final Uri uri = fetchState.getUri();

        try {
            Request request = new Request.Builder()
                    .url(uri.toString())
                    .get()
                    .build();

            fetchWithRequest(fetchState, callback, request);
        } catch (Exception e) {
            // handle error while creating the request
            callback.onFailure(e);
        }
    }
}
