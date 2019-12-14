package org.wikipedia.dataclient.page;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
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
@SuppressWarnings("unused")
public class PageSummary {
    public static final String TYPE_STANDARD = "standard";
    public static final String TYPE_DISAMBIGUATION = "disambiguation";
    public static final String TYPE_MAIN_PAGE = "mainpage";
    public static final String TYPE_NO_EXTRACT = "no-extract";

    @Nullable private String type;
    @Nullable private Titles titles;
    @SuppressWarnings("unused,NullableProblems") @Required
    @NonNull private String title;
    @Nullable private NamespaceContainer namespace;
    @Nullable private String extract;
    @Nullable @SerializedName("extract_html") private String extractHtml;
    @Nullable private String description;
    @Nullable private Thumbnail thumbnail;
    @Nullable @SerializedName("originalimage") private Thumbnail originalImage;
    @Nullable private String lang;
    private int pageid;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String displaytitle;
    @SuppressWarnings("unused") @Nullable private String redirected;
    @SuppressWarnings("unused") private long revision;
    @SuppressWarnings("unused") @Nullable @JsonAdapter(GeoTypeAdapter.class) private Location coordinates;
    @SuppressWarnings("unused") @Nullable private String timestamp;
    @SerializedName("wikibase_item") @Nullable private String wikiBaseItem;

    public Page toPage(PageTitle title, String leadImageName, String leadImageUrl) {
        return new Page(adjustPageTitle(title),
                toPageProperties(leadImageName, leadImageUrl));
    }

    private PageTitle adjustPageTitle(PageTitle title) {
        if (titles != null && titles.canonical != null) {
            title = new PageTitle(titles.canonical, title.getWikiSite(), title.getThumbUrl());
        }
        title.setDescription(description);
        return title;
    }

    private PageProperties toPageProperties(String leadImageName, String leadImageUrl) {
        return new PageProperties(this, leadImageName, leadImageUrl);
    }


    @NonNull
    public String getApiTitle() {
        return StringUtils.defaultString(titles != null ? titles.canonical : null);
    }

    // TODO: Make this return CharSequence, and automatically convert from HTML.
    @NonNull
    public String getDisplayTitle() {
        return StringUtils.defaultString(titles != null ? titles.display : null);
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

    @Nullable
    public String getOriginalImageUrl() {
        return originalImage == null ? null : originalImage.getUrl();
    }

    @NonNull
    public PageTitle getPageTitle(@NonNull WikiSite wiki) {
        return new PageTitle(getApiTitle(), wiki, getThumbnailUrl(), getDescription());
    }

    public int getPageId() {
        return pageid;
    }

    @NonNull
    public String getLang() {
        return StringUtils.defaultString(lang);
    }

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

    private static class Titles {
        @Nullable private String canonical;
        @Nullable private String display;
    }

    @Override @NonNull public String toString() {
        return getDisplayTitle();
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
