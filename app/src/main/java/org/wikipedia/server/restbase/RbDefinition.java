package org.wikipedia.server.restbase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;

import org.wikipedia.util.log.L;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class RbDefinition {
    @Expose @Nullable private RbServiceError error;

    @Expose @NonNull private Usage[] usages;

    @NonNull public Usage[] getUsages() {
        return usages;
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
        @Expose @NonNull private String partOfSpeech;
        @Expose @NonNull private Definition[] definitions;

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
        @Expose @NonNull private String definition;
        @Expose @Nullable private String[] examples;

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
