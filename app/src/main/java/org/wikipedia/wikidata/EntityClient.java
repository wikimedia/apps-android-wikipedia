package org.wikipedia.wikidata;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;

import retrofit2.Call;
import retrofit2.Response;

public final class EntityClient {
    public static final String WIKIDATA_WIKI = "wikidatawiki";

    public interface LabelCallback {
        void success(@NonNull String label);
        void failure(@NonNull Throwable t);
    }

    @VisibleForTesting
    static class LabelCallbackAdapter implements retrofit2.Callback<Entities> {
        @NonNull private final LabelCallback callback;
        @NonNull private final String qNumber;
        @NonNull private final String langCode;

        LabelCallbackAdapter(@NonNull LabelCallback callback, @NonNull final String qNumber,
                             @NonNull final String langCode) {
            this.callback = callback;
            this.qNumber = qNumber;
            this.langCode = langCode;
        }

        @Override public void onResponse(@NonNull Call<Entities> call, @NonNull Response<Entities> response) {
            if (response.body() != null) {
                for (Entities.Entity item : response.body().entities().values()) {
                    if (item.id().equals(qNumber)) {
                        for (Entities.Label label : item.labels().values()) {
                            if (label.language().equals(langCode)) {
                                callback.success(label.value());
                                return;
                            }
                        }
                    }
                }
            }
            callback.failure(new JsonParseException("Failed to find label for " + qNumber + ":" + langCode));
        }

        @Override public void onFailure(@NonNull Call<Entities> call, @NonNull Throwable caught) {
            callback.failure(caught);
        }
    }

    public void getLabelForLang(@NonNull final String qNumber, @NonNull final String langCode,
                                @NonNull final LabelCallback callback) {
        requestLabels(ServiceFactory.get(new WikiSite(Service.WIKIDATA_URL)), qNumber, langCode).enqueue(new LabelCallbackAdapter(callback, qNumber, langCode));
    }

    @VisibleForTesting @NonNull
    Call<Entities> requestLabels(@NonNull Service service, @NonNull final String qNumber,
                                 @NonNull final String langCode) {
        return service.getWikidataLabels(qNumber, langCode);
    }
}
