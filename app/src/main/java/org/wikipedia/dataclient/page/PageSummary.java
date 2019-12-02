package org.wikipedia.dataclient.page;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.annotations.Required;
import org.wikipedia.page.GeoTypeAdapter;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;

/**
 * Represents a summary of a page, useful for page previews.
 */
public class PageSummary {
    public static final String TYPE_STANDARD = "standard";
    public static final String TYPE_DISAMBIGUATION = "disambiguation";
    public static final String TYPE_MAIN_PAGE = "mainpage";
    public static final String TYPE_NO_EXTRACT = "no-extract";

    @SuppressWarnings("unused") @Nullable private String type;
    @SuppressWarnings("unused,NullableProblems") @Required
    @NonNull private String title;
    @SuppressWarnings("unused") @Nullable private String normalizedtitle;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String displaytitle;
    @SuppressWarnings("unused") @Nullable private NamespaceContainer namespace;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable @SerializedName("extract_html") private String extractHtml;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable @SerializedName("originalimage") private Thumbnail originalImage;
    @SuppressWarnings("unused") @Nullable private String lang;
    @SuppressWarnings("unused") private int pageid;
    @SuppressWarnings("unused") @Nullable private String redirected;
    @SuppressWarnings("unused") private long revision;
    @SuppressWarnings("unused") @Nullable @JsonAdapter(GeoTypeAdapter.class) private Location coordinates;
    @SuppressWarnings("unused") @Nullable private String timestamp;
    @SuppressWarnings("unused") @Nullable @SerializedName("wikibase_item") private String wikiBaseItem;

    public Page toPage(PageTitle title, String leadImageName, String leadImageUrl, String titlePronunciationUrl) {
        return new Page(adjustPageTitle(title),
                toPageProperties(leadImageName, leadImageUrl, titlePronunciationUrl));
    }

    private PageTitle adjustPageTitle(PageTitle title) {
        if (redirected != null) {
            // Handle redirects properly.
            title = new PageTitle(redirected, title.getWikiSite(), title.getThumbUrl());
        } else if (normalizedtitle != null) {
            // We care about the normalized title only if we were not redirected
            title = new PageTitle(normalizedtitle, title.getWikiSite(), title.getThumbUrl());
        }
        title.setDescription(description);
        return title;
    }

    private PageProperties toPageProperties(String leadImageName, String leadImageUrl, String titlePronunciationUrl) {
        return new PageProperties(this, leadImageName, leadImageUrl, titlePronunciationUrl);
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    @NonNull
    public String getDisplayTitle() {
        return displaytitle;
    }

    @NonNull
    public String getConvertedTitle() {
        return title;
    }

    @NonNull
    public Namespace getNamespace() {
        return namespace == null ? Namespace.MAIN : Namespace.of(namespace.id());
    }

    @NonNull
    public String getType() {
        return TextUtils.isEmpty(type) ? TYPE_STANDARD : type;
    }

    @Nullable
    public String getExtract() {
        return extract;
    }

    @Nullable
    public String getExtractHtml() {
        return extractHtml;
    }

    @Nullable
    public String getThumbnailUrl() {
        return thumbnail == null ? null : thumbnail.getUrl();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getNormalizedTitle() {
        return normalizedtitle == null ? title : normalizedtitle;
    }

    @Nullable
    public String getOriginalImageUrl() {
        return originalImage == null ? null : originalImage.getUrl();
    }

    @NonNull
    public PageTitle getPageTitle(@NonNull WikiSite wiki) {
        return new PageTitle(getTitle(), wiki, getThumbnailUrl(), getDescription());
    }

    public int getPageId() {
        return pageid;
    }

    public String getLang() {
        return lang;
    }

    /**
     * For the thumbnail URL of the page
     */
    private static class Thumbnail {
        @SuppressWarnings("unused") private String source;

        public String getUrl() {
            return source;
        }
    }

    private static class NamespaceContainer {
        @SuppressWarnings("unused") private int id;
        @SuppressWarnings("unused") @Nullable private String text;

        public int id() {
            return id;
        }
    }

    @Override public String toString() {
        return getTitle();
    }

    public long getRevision() {
        return revision;
    }

    @Nullable
    public Location getGeo() {
        return coordinates;
    }

    @Nullable
    public String getTimestamp() {
        return timestamp;
    }

    @Nullable
    public String getWikiBaseItem() {
        return wikiBaseItem;
    }
}
