package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.gallery.VideoInfo;
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
    @SuppressWarnings("unused") @Nullable private List<Category> categories;
    @SuppressWarnings("unused") @Nullable private PageProps pageprops;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @SerializedName("descriptionsource") @Nullable private String descriptionSource;
    @SuppressWarnings("unused") @SerializedName("imageinfo") @Nullable private List<ImageInfo> imageInfo;
    @SuppressWarnings("unused") @SerializedName("videoinfo") @Nullable private List<VideoInfo> videoInfo;
    @Nullable private String redirectFrom;
    @Nullable private String convertedFrom;
    @Nullable private String convertedTo;

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

    @Nullable public List<Category> categories() {
        return categories;
    }

    @Nullable public List<Coordinates> coordinates() {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        if (coordinates != null) {
            coordinates.removeAll(Collections.singleton(null));
        }
        return coordinates;
    }

    public int pageId() {
        return pageid;
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

    @Nullable public String convertedTo() {
        return convertedTo;
    }

    public void convertedTo(@Nullable String to) {
        convertedTo = to;
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
        @SuppressWarnings("unused") @Nullable private Double lat;
        @SuppressWarnings("unused") @Nullable private Double lon;

        @Nullable public Double lat() {
            return lat;
        }
        @Nullable public Double lon() {
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
        @SuppressWarnings("unused") @Nullable private String disambiguation;

        @Nullable public String getDisplayTitle() {
            return displaytitle;
        }

        @NonNull public String getWikiBaseItem() {
            return StringUtils.defaultString(wikiBaseItem);
        }

        public boolean isDisambiguation() {
            return disambiguation != null;
        }
    }

    public static class Category {
        @SuppressWarnings("unused") private int ns;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String title;
        @SuppressWarnings("unused") private boolean hidden;

        public int ns() {
            return ns;
        }

        @NonNull public String title() {
            return StringUtils.defaultString(title);
        }

        public boolean hidden() {
            return hidden;
        }
    }
}
