package org.wikipedia.gallery;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Gson POJO for a standard video info object as returned by the API VideoInfo module via
 * GalleryCollectionClient's request
 */
public class VideoInfo extends ImageInfo {

    @SuppressWarnings("unused") @SerializedName("derivatives") @Nullable private List<Derivative> derivatives;

    @Nullable List<Derivative> getDerivatives() {
        return derivatives;
    }
}
