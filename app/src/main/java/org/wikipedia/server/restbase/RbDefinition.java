package org.wikipedia.server.restbase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.util.log.L;

import java.util.Map;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class RbDefinition {

    @NonNull private Map<String, Usage[]> usagesByLang;

    @Nullable private RbServiceError error;

    public RbDefinition(Map<String, RbDefinition.Usage[]> usages) {
        usagesByLang = usages;
    }

    public Map<String, Usage[]> getUsagesByLang() {
        return usagesByLang;
    }

    @Nullable public Usage[] getUsagesForLang(String langCode) {
        return usagesByLang.get(langCode);
    }

    public boolean hasError() {
        return error != null;
    }

    public void logError(String message) {
        if (error != null) {
            message += ": " + error.toString();
        }
        L.e(message);
    }

    public interface Callback {
        void success(RbDefinition definition, Response response);

        void failure(RetrofitError error);
    }

    public static class Usage {
        @NonNull private String partOfSpeech;
        @NonNull private Definition[] definitions;

        public Usage(@NonNull String partOfSpeech, @NonNull Definition[] definitions) {
            this.partOfSpeech = partOfSpeech;
            this.definitions = definitions;
        }

        @NonNull public String getPartOfSpeech() {
            return partOfSpeech;
        }

        @NonNull public Definition[] getDefinitions() {
            return definitions;
        }
    }

    public static class Definition {
        @NonNull private String definition;
        @Nullable private String[] examples;

        public Definition(@NonNull String definition, @Nullable String[] examples) {
            this.definition = definition;
            this.examples = examples;
        }

        @NonNull public String getDefinition() {
            return definition;
        }

        @Nullable public String[] getExamples() {
            return examples;
        }
    }
}
