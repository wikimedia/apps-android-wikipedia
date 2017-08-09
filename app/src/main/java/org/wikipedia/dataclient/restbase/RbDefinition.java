package org.wikipedia.dataclient.restbase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.json.annotations.Required;

import java.util.Map;

public class RbDefinition {
    @Required @NonNull private Map<String, Usage[]> usagesByLang;

    public RbDefinition(@NonNull Map<String, RbDefinition.Usage[]> usages) {
        usagesByLang = usages;
    }

    @Nullable public Usage[] getUsagesForLang(String langCode) {
        return usagesByLang.get(langCode);
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
