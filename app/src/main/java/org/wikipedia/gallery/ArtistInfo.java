package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

public class ArtistInfo extends TextInfo {

    @SuppressWarnings("unused,NullableProblems") @Nullable private String name;
    @SuppressWarnings("unused,NullableProblems") @Nullable @SerializedName("user_page") private String userPage;

    @NonNull
    public String getName() {
        return StringUtils.defaultString(name);
    }
}
