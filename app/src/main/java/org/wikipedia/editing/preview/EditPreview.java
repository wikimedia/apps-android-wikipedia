package org.wikipedia.editing.preview;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.model.BaseModel;

public class EditPreview extends BaseModel {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Parse parse;
    @NonNull protected String result() {
        return parse.text().result();
    }

    static class Parse {
        @SuppressWarnings("unused,NullableProblems") @NonNull private Text text;
        @NonNull Text text() {
            return text;
        }
    }

    static class Text {
        @SuppressWarnings("unused,NullableProblems") @SerializedName("*") @NonNull private String result;
        @NonNull String result() {
            return result;
        }
    }
}
