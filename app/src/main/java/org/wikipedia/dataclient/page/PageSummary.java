package org.wikipedia.dataclient.page;

import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.GeoTypeAdapter;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.UriUtil;

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
    @Nullable private NamespaceContainer namespace;
    @Nullable private String extract;
    @Nullable @SerializedName("extract_html") private String extractHtml;
    @Nullable private String description;
    @Nullable @SerializedName("description_source") private String descriptionSource;
    @Nullable private Thumbnail thumbnail;
    @Nullable @SerializedName("originalimage") private Thumbnail originalImage;
    @Nullable private String lang;
    private int pageid;

    private long revision;
    @Nullable @JsonAdapter(GeoTypeAdapter.class) private Location coordinates;
    @Nullable private String timestamp;
    @SerializedName("wikibase_item") @Nullable private String wikiBaseItem;

    public PageSummary() { }

    public PageSummary(@NonNull String displayTitle, @NonNull String prefixTitle, @Nullable String description,
                       @Nullable String extract, @Nullable String thumbnail, @NonNull String lang) {
        this.titles = new Titles(prefixTitle, displayTitle);
        this.description = description;
        this.extract = extract;
        this.thumbnail = new Thumbnail(thumbnail, 0, 0);
        this.lang = lang;
    }

    public Page toPage(PageTitle title) {
        return new Page(adjustPageTitle(title), new PageProperties(this));
    }

    private PageTitle adjustPageTitle(PageTitle title) {
        PageTitle newTitle = title;
        if (titles != null && titles.canonical != null) {
            newTitle = new PageTitle(titles.canonical, title.getWikiSite(), title.getThumbUrl());
            newTitle.setFragment(title.getFragment());
        }
        newTitle.setDescription(description);
        return newTitle;
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

    public int getThumbnailWidth() {
        return thumbnail == null ? 0 : thumbnail.getWidth();
    }

    public int getThumbnailHeight() {
        return thumbnail == null ? 0 : thumbnail.getHeight();
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getDescriptionSource() {
        return StringUtils.defaultString(descriptionSource);
    }

    @Nullable
    public String getOriginalImageUrl() {
        return originalImage == null ? null : originalImage.getUrl();
    }

    @NonNull
    public PageTitle getPageTitle(@NonNull WikiSite wiki) {
        return new PageTitle(getApiTitle(), wiki, getThumbnailUrl(), getDescription(), getDisplayTitle(), getExtract());
    }

    public int getPageId() {
        return pageid;
    }

    @NonNull
    public String getLang() {
        return StringUtils.defaultString(lang);
    }

    private static class Thumbnail {
        private final String source;
        private final int width;
        private final int height;

        Thumbnail(@Nullable String source, int width, int height) {
            this.source = source;
            this.width = width;
            this.height = height;
        }

        public String getUrl() {
            return source;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    private static class NamespaceContainer {
        private int id;
        @Nullable private String text;

        public int id() {
            return id;
        }
    }

    private static class Titles {
        @Nullable private String canonical;
        @Nullable private String display;

        Titles(@Nullable String canonical, @Nullable String display) {
            this.canonical = canonical;
            this.display = display;
        }
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
        return StringUtils.defaultString(timestamp);
    }

    @Nullable
    public String getWikiBaseItem() {
        return StringUtils.defaultString(wikiBaseItem);
    }

    @Nullable
    public String getLeadImageName() {
        if (getThumbnailUrl() == null) {
            return null;
        }
        return UriUtil.getFilenameFromUploadUrl(getThumbnailUrl());
    }
}
