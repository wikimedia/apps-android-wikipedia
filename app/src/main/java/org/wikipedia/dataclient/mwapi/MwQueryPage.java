package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.gallery.VideoInfo;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.Namespace;

import java.util.Collections;
import java.util.List;

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
public class MwQueryPage extends BaseModel {
    @SuppressWarnings("unused") private int pageid;
    @SuppressWarnings("unused") private int ns;
    @SuppressWarnings("unused") private int index;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
    @SuppressWarnings("unused") @Nullable private List<LangLink> langlinks;
    @SuppressWarnings("unused") @Nullable private List<Revision> revisions;
    @SuppressWarnings("unused") @Nullable private List<Coordinates> coordinates;
    @SuppressWarnings("unused") @Nullable private PageProps pageprops;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @SerializedName("descriptionsource") @Nullable private String descriptionSource;
    @SuppressWarnings("unused") @SerializedName("imageinfo") @Nullable private List<ImageInfo> imageInfo;
    @SuppressWarnings("unused") @SerializedName("videoinfo") @Nullable private List<VideoInfo> videoInfo;
    @Nullable private String redirectFrom;
    @Nullable private String convertedFrom;

    @NonNull public String title() {
        return title;
    }

    public int index() {
        return index;
    }

    @NonNull public Namespace namespace() {
        return Namespace.of(ns);
    }

    @Nullable public List<LangLink> langLinks() {
        return langlinks;
    }

    @Nullable public List<Revision> revisions() {
        return revisions;
    }

    @Nullable public List<Coordinates> coordinates() {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        if (coordinates != null) {
            coordinates.removeAll(Collections.singleton(null));
        }
        return coordinates;
    }

    @Nullable public PageProps pageProps() {
        return pageprops;
    }

    @Nullable public String extract() {
        return extract;
    }

    @Nullable public String thumbUrl() {
        return thumbnail != null ? thumbnail.source() : null;
    }

    @Nullable public String description() {
        return description;
    }

    @Nullable
    public String descriptionSource() {
        return descriptionSource;
    }

    @Nullable public ImageInfo imageInfo() {
        return imageInfo != null ? imageInfo.get(0) : null;
    }

    @Nullable public VideoInfo videoInfo() {
        return videoInfo != null ? videoInfo.get(0) : null;
    }

    @Nullable public String redirectFrom() {
        return redirectFrom;
    }

    public void redirectFrom(@Nullable String from) {
        redirectFrom = from;
    }

    @Nullable public String convertedFrom() {
        return convertedFrom;
    }

    public void convertedFrom(@Nullable String from) {
        convertedFrom = from;
    }

    public void appendTitleFragment(@Nullable String fragment) {
        title += "#" + fragment;
    }

    public static class Revision {
        @SuppressWarnings("unused,NullableProblems") @SerializedName("contentformat") @NonNull private String contentFormat;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("contentmodel") @NonNull private String contentModel;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("timestamp") @NonNull private String timeStamp;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String content;

        @NonNull public String content() {
            return content;
        }

        @NonNull public String timeStamp() {
            return StringUtils.defaultString(timeStamp);
        }
    }

    public static class LangLink {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String lang;
        @NonNull public String lang() {
            return lang;
        }
        @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
        @NonNull public String title() {
            return title;
        }
    }

    public static class Coordinates {
        // Use Double object type rather than primitive type so that the presence of the fields can
        // be checked correctly by the RequiredFieldsCheckOnReadTypeAdapter.
        @SuppressWarnings("unused") @Required @NonNull private Double lat;
        @SuppressWarnings("unused") @Required @NonNull private Double lon;

        public Coordinates(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double lat() {
            return lat;
        }
        public double lon() {
            return lon;
        }
    }

    static class Thumbnail {
        @SuppressWarnings("unused") private String source;
        @SuppressWarnings("unused") private int width;
        @SuppressWarnings("unused") private int height;
        String source() {
            return source;
        }
    }

    public static class PageProps {
        @SuppressWarnings("unused") @SerializedName("wikibase_item") @Nullable private String wikiBaseItem;
        @SuppressWarnings("unused") @Nullable private String displaytitle;

        @Nullable public String getDisplayTitle() {
            return displaytitle;
        }

        @Nullable public String getWikiBaseItem() {
            return wikiBaseItem;
        }
    }
}
