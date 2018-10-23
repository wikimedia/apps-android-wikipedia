package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.Service;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.StringUtil;

import java.io.Serializable;
import java.util.List;

import static org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE;

public class GalleryItem implements Serializable {
    @SuppressWarnings("unused") @SerializedName("section_id") private int sectionId;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String type;
    @SuppressWarnings("unused,NullableProblems") @Nullable @SerializedName("audio_type") private String audioType;
    @SuppressWarnings("unused") @Nullable private TextInfo caption;
    @SuppressWarnings("unused") private boolean showInGallery;
    @SuppressWarnings("unused") @NonNull private Titles titles;
    @SuppressWarnings("unused") @Nullable private ImageInfo thumbnail;
    @SuppressWarnings("unused") @Nullable private ImageInfo original;
    @SuppressWarnings("unused") @Nullable private List<VideoInfo> sources;
    @SuppressWarnings("unused,NullableProblems") @Nullable @SerializedName("file_page") private String filePage;
    @SuppressWarnings("unused") @Nullable private ArtistInfo artist;
    @SuppressWarnings("unused") private double duration;
    @SuppressWarnings("unused") @NonNull private ImageLicense license;
    @SuppressWarnings("unused") @Nullable private TextInfo description;
    // FIXME: The type of credit will return either string or another type of object
    // @SuppressWarnings("unused") @Nullable private String credit;

    public GalleryItem(@NonNull String title) {
        this.type = "*/*";
        this.titles = new Titles(title, StringUtil.addUnderscores(title), title);
        this.original = new ImageInfo();
        this.thumbnail = new ImageInfo();
        this.description = new TextInfo();
        this.license = new ImageLicense();
    }

    @NonNull
    public String getType() {
        return StringUtils.defaultString(type);
    }

    @NonNull
    public String getAudioType() {
        return StringUtils.defaultString(audioType);
    }

    @Nullable
    public TextInfo getCaption() {
        return caption;
    }

    public boolean isShowInGallery() {
        return showInGallery;
    }

    @NonNull
    public Titles getTitles() {
        return titles;
    }

    @NonNull
    public ImageInfo getThumbnail() {
        if (thumbnail == null) {
            thumbnail = new ImageInfo();
        }
        return thumbnail;
    }

    @NonNull
    public String getThumbnailUrl() {
        return getThumbnail().getSource();
    }

    @NonNull
    public String getPreferredSizedImageUrl() {
        return ImageUrlUtil.getUrlForPreferredSize(getThumbnailUrl(), PREFERRED_GALLERY_IMAGE_SIZE);
    }

    @NonNull
    public ImageInfo getOriginal() {
        if (original == null) {
            original = new ImageInfo();
        }
        return original;
    }

    @Nullable
    public List<VideoInfo> getSources() {
        return sources;
    }

    @Nullable
    public VideoInfo getOriginalVideoSource() {
        // TODO: the getSources has different levels of source,
        // should have an option that allows user to chose which quality to play
        return sources == null || sources.size() == 0
                ? null : sources.get(sources.size() - 1);
    }

    public double getDuration() {
        return duration;
    }

    @NonNull
    public String getFilePage() {
        // return the base url of Wiki Commons for WikiSite() if the file_page is null.
        return filePage == null ? Service.COMMONS_URL : StringUtils.defaultString(filePage);
    }

    @Nullable
    public ArtistInfo getArtist() {
        return artist;
    }

    @NonNull
    public ImageLicense getLicense() {
        return license;
    }

    @NonNull
    public TextInfo getDescription() {
        if (description == null) {
            description = new TextInfo();
        }
        return description;
    }

    // TODO: Move the following models into a folder

    public static class Titles implements Serializable {
        @SuppressWarnings("unused,NullableProblems") @Nullable private String canonical;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String normalized;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String display;

        Titles(@NonNull String display, @NonNull String canonical, @NonNull String normalized) {
            this.display = display;
            this.canonical = canonical;
            this.normalized = normalized;
        }

        @NonNull
        public String getCanonical() {
            return StringUtils.defaultString(canonical);
        }

        @NonNull
        public String getNormalized() {
            return StringUtils.defaultString(normalized);
        }

        @NonNull
        public String getDisplay() {
            return StringUtils.defaultString(display);
        }
    }
}
