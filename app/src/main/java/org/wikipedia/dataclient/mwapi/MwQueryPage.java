package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.gallery.VideoInfo;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.Namespace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
@SuppressWarnings("unused")
public class MwQueryPage extends BaseModel {
    private int pageid;
    private int ns;
    private int index;
    @Nullable private String title;
    @Nullable private List<LangLink> langlinks;
    @Nullable private List<Revision> revisions;
    @Nullable private List<Coordinates> coordinates;
    @Nullable private List<Category> categories;
    @Nullable private PageProps pageprops;
    @Nullable private String extract;
    @Nullable private Thumbnail thumbnail;
    @Nullable private String description;
    @SerializedName("descriptionsource") @Nullable private String descriptionSource;
    @SerializedName("imageinfo") @Nullable private List<ImageInfo> imageInfo;
    @SerializedName("videoinfo") @Nullable private List<VideoInfo> videoInfo;
    @Nullable private String redirectFrom;
    @Nullable private String convertedFrom;
    @Nullable private String convertedTo;
    @Nullable private Map<String, String> varianttitles;

    @NonNull public String title() {
        return StringUtils.defaultString(title);
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

    @NonNull public String displayTitle(@NonNull String langCode) {
        return varianttitles != null ? StringUtils.defaultIfEmpty(varianttitles.get(langCode), title()) : title();
    }

    public static class Revision {
        @SerializedName("contentformat") @Nullable private String contentFormat;
        @SerializedName("contentmodel") @Nullable private String contentModel;
        @SerializedName("timestamp") @Nullable private String timeStamp;
        @Nullable private String content;

        @NonNull public String content() {
            return StringUtils.defaultString(content);
        }

        @NonNull public String timeStamp() {
            return StringUtils.defaultString(timeStamp);
        }
    }

    public static class LangLink {
        @Nullable private String lang;
        @NonNull public String lang() {
            return StringUtils.defaultString(lang);
        }
        @Nullable private String title;
        @NonNull public String title() {
            return StringUtils.defaultString(title);
        }
    }

    public static class Coordinates {
        @Nullable private Double lat;
        @Nullable private Double lon;

        @Nullable public Double lat() {
            return lat;
        }
        @Nullable public Double lon() {
            return lon;
        }
    }

    static class Thumbnail {
        private String source;
        private int width;
        private int height;
        String source() {
            return source;
        }
    }

    public static class PageProps {
        @SerializedName("wikibase_item") @Nullable private String wikiBaseItem;
        @Nullable private String displaytitle;
        @Nullable private String disambiguation;

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
        private int ns;
        @Nullable private String title;
        private boolean hidden;

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
