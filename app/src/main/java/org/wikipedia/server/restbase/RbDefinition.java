package org.wikipedia.server.restbase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.json.annotations.Required;
import org.wikipedia.util.log.L;

import java.util.Map;

public class RbDefinition {
    @Required @NonNull private Map<String, Usage[]> usagesByLang;
    @SuppressWarnings("unused") @Nullable private RbServiceError error;

    RbDefinition(@NonNull Map<String, RbDefinition.Usage[]> usages) {
        usagesByLang = usages;
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

    public static class Usage {
        @Required @NonNull private String partOfSpeech;
        @Required @NonNull private Definition[] definitions;

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
        @Required @NonNull private String definition;
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
