package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.wikipedia.WikipediaApp;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    // TODO: replace static file with dataclient when service is set up
    public void request(@NonNull Callback cb) {
        try {
            cb.success(getRemoteZimInfo());
        } catch (IOException e) {
            cb.error(e);
            L.e(e);
        }
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

    @NonNull
    private List<Compilation> getRemoteZimInfo() throws IOException {
        List<Compilation> result = new ArrayList<>();
        LineIterator i = IOUtils.lineIterator(openAssetFile("zims.tsv"), "UTF-8");
        while (i.hasNext()) {
            result.add(new Compilation(i.nextLine().split("\t")));
        }
        i.close();
        return result;
    }

    @NonNull
    private static InputStream openAssetFile(String path) throws IOException {
        return WikipediaApp.getInstance().getAssets().open(path);
    }
}
