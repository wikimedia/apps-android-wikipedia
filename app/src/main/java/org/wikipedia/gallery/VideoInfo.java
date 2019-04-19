package org.wikipedia.gallery;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Gson POJO for a standard video info object as returned by the API VideoInfo module
 */
public class VideoInfo extends ImageInfo {
    @SuppressWarnings("unused") @Nullable private List<String> codecs;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String name;
    @SuppressWarnings("unused,NullableProblems") @Nullable @SerializedName("short_name") private String shortName;
}
