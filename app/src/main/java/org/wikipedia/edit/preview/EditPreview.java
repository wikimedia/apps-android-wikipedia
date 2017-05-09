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
        return parse != null ? parse.text() : null;
    }

    private static class Parse {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
        @SuppressWarnings("unused") @SerializedName("pageid") private int pageId;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String text;
        @NonNull String text() {
            return text;
        }
    }
}
