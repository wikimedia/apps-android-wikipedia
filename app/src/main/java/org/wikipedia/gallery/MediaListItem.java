package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("unused")
public class MediaListItem implements Serializable {
    @Nullable private String title;
    @SerializedName("section_id") private int sectionId;
    @Nullable private String type;
    @Nullable private TextInfo caption;
    @Nullable private List<VideoInfo> sources;
    private boolean showInGallery;

    public MediaListItem() {
    }

    @NonNull
    public String getType() {
        return StringUtils.defaultString(type);
    }

    @Nullable
    public TextInfo getCaption() {
        return caption;
    }

    public boolean showInGallery() {
        return showInGallery;
    }

    @NonNull
    public String getTitle() {
        return StringUtils.defaultString(title);
    }

    @Nullable
    public List<VideoInfo> getSources() {
        return sources;
    }

    @Nullable
    public VideoInfo getOriginalVideoSource() {
        // The getSources has different levels of source,
        // should have an option that allows user to chose which quality to play
        return sources == null || sources.size() == 0
                ? null : sources.get(sources.size() - 1);
    }
}
