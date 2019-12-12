package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class MediaListItem implements Serializable {
    @Nullable private String title;
    @SerializedName("section_id") private int sectionId;
    @Nullable private String type;
    @Nullable private TextInfo caption;
    private boolean showInGallery;
    @Nullable @SerializedName("srcset") private List<ImageSrcSet> srcSets;

    public MediaListItem() {
    }

    public MediaListItem(@NonNull String title) {
        this.title = title;
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

    @NonNull
    public List<ImageSrcSet> getSrcSets() {
        return srcSets != null ? srcSets : Collections.emptyList();
    }

    @NonNull
    public String getImageUrl(int preferredSize) {
        Pattern pattern = Pattern.compile("/(\\d+)px-");
        String imageUrl = getSrcSets().get(0).getSrc();
        int lastSizeDistance = Integer.MAX_VALUE;
        for (ImageSrcSet srcSet : getSrcSets()) {
            Matcher matcher = pattern.matcher(srcSet.getSrc());
            if (matcher.find() && matcher.group(1) != null) {
                int currentSizeDistance = Math.abs(Integer.parseInt(matcher.group(1)) - preferredSize);
                if (currentSizeDistance < lastSizeDistance) {
                    imageUrl = srcSet.getSrc();
                    lastSizeDistance = currentSizeDistance;
                }
            }
        }
        return imageUrl;
    }

    public class ImageSrcSet implements Serializable {
        @Nullable private String src;
        @Nullable private String scale;

        @NonNull
        public String getSrc() {
            return StringUtils.defaultString(src);
        }
    }
}
