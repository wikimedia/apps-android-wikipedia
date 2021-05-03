package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.page.Protection;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.page.Namespace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
@SuppressWarnings("unused")
public class MwQueryPage {
    private int pageid;
    private int ns;
    private int index;
    private long lastrevid;
    @Nullable private String title;
    @Nullable private List<LangLink> langlinks;
    @Nullable private List<Revision> revisions;
    @Nullable private List<Coordinates> coordinates;
    @Nullable private List<Category> categories;
    @Nullable private List<Protection> protection;
    @Nullable private PageProps pageprops;
    @Nullable private String extract;
    @Nullable private Thumbnail thumbnail;
    @Nullable private String description;
    @SerializedName("descriptionsource") @Nullable private String descriptionSource;
    @SerializedName("imageinfo") @Nullable private List<ImageInfo> imageInfo;
    @SerializedName("videoinfo") @Nullable private List<ImageInfo> videoInfo;
    @Nullable private String imagerepository;
    @Nullable private String redirectFrom;
    @Nullable private String convertedFrom;
    @Nullable private String convertedTo;
    @Nullable private Map<String, String> varianttitles;
    @SerializedName("pageviews") @Nullable private Map<String, Long> pageViewsMap;
    @SerializedName("imagelabels") @Nullable private List<ImageLabel> imageLabels;
    @SerializedName("watchlistexpiry") @Nullable private String watchlistExpiry;
    @Nullable private  Map<String, List<MwServiceError>> actions;
    private boolean watched;


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

    @NonNull public List<Revision> revisions() {
        return revisions != null ? revisions : Collections.emptyList();
    }

    @Nullable public List<Category> categories() {
        return categories;
    }

    @NonNull public List<Protection> protection() {
        return protection == null ? Collections.emptyList() : protection;
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
        return imageInfo != null ? imageInfo.get(0) : videoInfo != null ? videoInfo.get(0) : null;
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

    @NonNull public Map<String, Long> getPageViewsMap() {
        return pageViewsMap != null ? pageViewsMap : Collections.emptyMap();
    }

    @NonNull public List<ImageLabel> getImageLabels() {
        return imageLabels != null ? imageLabels : Collections.emptyList();
    }

    public boolean isImageShared() {
        return StringUtils.defaultString(imagerepository).equals("shared");
    }

    public boolean hasWatchlistExpiry() {
        return !TextUtils.isEmpty(watchlistExpiry);
    }

    public boolean isWatched() {
        return watched;
    }

    public long getLastRevId() {
        return lastrevid;
    }

    @Nullable public List<MwServiceError> getErrorForAction(String actionName) {
        return (actions != null && actions.containsKey(actionName)) ? actions.get(actionName) : Collections.emptyList();
    }

    public static class Revision {
        private long revid;
        private long parentid;
        private boolean minor;
        private boolean anon;
        @Nullable private String user;
        @SerializedName("contentformat") @Nullable private String contentFormat;
        @SerializedName("contentmodel") @Nullable private String contentModel;
        @SerializedName("timestamp") @Nullable private String timeStamp;
        @Nullable private String content;
        @Nullable private String comment;
        @Nullable private Map<String, RevisionSlot> slots;

        @NonNull public String getComment() {
            return StringUtils.defaultString(comment);
        }

        public long getRevId() {
            return revid;
        }

        public long getParentRevId() {
            return parentid;
        }

        @NonNull
        public String getUser() {
            return StringUtils.defaultString(user);
        }

        public boolean isAnon() {
            return anon;
        }

        @NonNull
        public String content() {
            return StringUtils.defaultString(content);
        }

        @NonNull public String timeStamp() {
            return StringUtils.defaultString(timeStamp);
        }

        @NonNull public String getContentFromSlot(@NonNull String slot) {
            return slots != null && slots.containsKey(slot) ? slots.get(slot).getContent() : "";
        }
    }

    public static class RevisionSlot {
        @Nullable private String contentmodel;
        @Nullable private String contentformat;
        @Nullable private String content;

        @NonNull public String getContent() {
            return StringUtils.defaultString(content);
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

    public static class ImageLabel {
        @SerializedName("wikidata_id") @Nullable private String wikidataId;
        @Nullable private Confidence confidence;
        @Nullable private String state;
        @Nullable private String label;
        @Nullable private String description;
        private boolean selected;
        private boolean custom;

        public ImageLabel() {
        }

        public ImageLabel(@NonNull String wikidataId, @NonNull String label, @Nullable String description) {
            this.wikidataId = wikidataId;
            this.label = label;
            this.description = description;
            custom = true;
        }

        @NonNull public String getWikidataId() {
            return StringUtils.defaultString(wikidataId);
        }

        @NonNull public String getState() {
            return StringUtils.defaultString(state);
        }

        @NonNull public String getLabel() {
            return StringUtils.defaultString(label);
        }

        @NonNull public String getDescription() {
            return StringUtils.defaultString(description);
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isCustom() {
            return custom;
        }

        public float getConfidenceScore() {
            return confidence == null ? 0 : confidence.getGoogle();
        }
    }

    public static class Confidence {
        private float google;

        public float getGoogle() {
            return google;
        }
    }


}
