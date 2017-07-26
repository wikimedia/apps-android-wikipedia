package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.settings.Prefs;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Headers;

import static org.wikipedia.Constants.ACCEPT_HEADER_PREFIX;

class CompilationClient {
    interface Callback {
        void success(@NonNull List<Compilation> compilations);
        void error(@NonNull Throwable caught);
    }

    @Nullable private Call<CallbackAdapter.CompilationResponse> call;

    // todo: according to Jake Wharton, Retrofit service objects are very expensive and should only
    // be created once for the application and then cached for reuse. Figure out how to make these
    // WikiSite-independent, and create once.
    // https://stackoverflow.com/a/20627010/5520737
    public void request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        cb.success(Compilation.getMockInfoForTesting()); // returning mock info for dev/testing

        /*cancel();
        Retrofit retrofit = RetrofitFactory.newInstance(getEndpoint(wiki), wiki);
        Service service = retrofit.create(Service.class);
        call = request(service);
        call.enqueue(new CallbackAdapter(cb));*/
    }

    public void cancel() {
        if (call == null) {
            return;
        }
        call.cancel();
        call = null;
    }

    @VisibleForTesting @NonNull
    Call<CallbackAdapter.CompilationResponse> request(@NonNull Service service) {
        return service.get();
    }

    @VisibleForTesting
    static class CallbackAdapter implements retrofit2.Callback<CallbackAdapter.CompilationResponse> {
        private Callback cb;

        CallbackAdapter(Callback cb) {
            this.cb = cb;
        }

        @Override
        public void onResponse(@NonNull Call<CompilationResponse> call,
                               @NonNull Response<CompilationResponse> response) {
            // noinspection ConstantConditions
            if (response.body() != null && response.body().compilations() != null) {
                // noinspection ConstantConditions
                cb.success(response.body().compilations());
            } else {
                cb.error(new IOException("An unknown error occurred."));
            }
        }

        @Override
        public void onFailure(@NonNull Call<CompilationResponse> call, @NonNull Throwable t) {
            cb.error(t);
        }

        static class CompilationResponse {
            @SuppressWarnings("unused,NullableProblems") @NonNull @Required
            private List<Compilation> compilations;

            @Nullable
            List<Compilation> compilations() {
                return compilations;
            }
        }
    }

    // todo: update endpoint path when finalized
    @VisibleForTesting
    interface Service {
        @NonNull @Headers(ACCEPT_HEADER_PREFIX + "compilations/0.1.0\"") @GET("/compilations")
        Call<CallbackAdapter.CompilationResponse> get();
    }

    private String getEndpoint(WikiSite wiki) {
        return String.format(Locale.ROOT, Prefs.getRestbaseUriFormat(), wiki.scheme(), wiki.authority());
    }
}
