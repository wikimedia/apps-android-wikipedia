package org.wikipedia.gallery;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ArtistInfo extends TextInfo {

    @SuppressWarnings("unused,NullableProblems") @Nullable private String name;
    @SuppressWarnings("unused,NullableProblems") @Nullable @SerializedName("user_page") private String userPage;

    @Nullable
    public String getName() {
        return name;
    }
}
