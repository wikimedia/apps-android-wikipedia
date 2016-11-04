package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.mwapi.MwPostResponse;

class EditPreview extends MwPostResponse {
    @SuppressWarnings("unused") @Nullable private Parse parse;

    boolean hasPreviewResult() {
        return parse != null;
    }

    @Nullable String result() {
        return parse != null ? parse.text().result() : null;
    }

    private static class Parse {
        @SuppressWarnings("unused,NullableProblems") @NonNull private Text text;
        @NonNull Text text() {
            return text;
        }
    }

    private static class Text {
        @SuppressWarnings("unused,NullableProblems") @SerializedName("*") @NonNull private String result;
        @NonNull String result() {
            return result;
        }
    }
}
