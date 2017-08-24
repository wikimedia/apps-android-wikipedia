package org.wikipedia.dataclient.restbase.page;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageLeadProperties;
import org.wikipedia.dataclient.page.Protection;
import org.wikipedia.page.GeoTypeAdapter;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.util.UriUtil;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.wikipedia.Constants.PREFERRED_THUMB_SIZE;

/**
 * Gson POJO for loading the first stage of page content.
 */
public class RbPageLead implements PageLead, PageLeadProperties {
    @SuppressWarnings("unused") private int id;
    @SuppressWarnings("unused") private long revision;
    @SuppressWarnings("unused") @Nullable private String lastmodified;
    @SuppressWarnings("unused") @Nullable private String displaytitle;
    @SuppressWarnings("unused") @Nullable private String redirected;
    @SuppressWarnings("unused") @Nullable private String normalizedtitle;
    @SuppressWarnings("unused") @Nullable @SerializedName("wikibase_item") private String wikiBaseItem;
    @SuppressWarnings("unused") @Nullable @SerializedName("pronunciation") private TitlePronunciation titlePronunciation;
    @SuppressWarnings("unused") @Nullable @JsonAdapter(GeoTypeAdapter.class) private Location geo;
    @SuppressWarnings("unused") private int languagecount;
    @SuppressWarnings("unused") private boolean editable;
    @SuppressWarnings("unused") private boolean mainpage;
    @SuppressWarnings("unused") private boolean disambiguation;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @Nullable private Image image;
    @SuppressWarnings("unused") @Nullable private Protection protection;
    @SuppressWarnings("unused") @Nullable private List<Section> sections;

    @Override
    public boolean hasError() {
        // If we have a page lead object, RESTBase hasn't returned an error
        return false;
    }

    @Override
    public ServiceError getError() {
        return null;
    }

    @Override
    public void logError(String message) {

    }

    /** Note: before using this check that #getMobileview != null */
    @Override
    public Page toPage(PageTitle title) {
        return new Page(adjustPageTitle(title),
                getSections(),
                toPageProperties(title.getWikiSite()));
    }

    /* package */ PageTitle adjustPageTitle(PageTitle title) {
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

    @Override
    public String getLeadSectionContent() {
        if (sections != null) {
            return sections.get(0).getContent();
        } else {
            return "";
        }
    }

    /** Converter */
    public PageProperties toPageProperties(@NonNull WikiSite wiki) {
        return new PageProperties(wiki, this);
    }

    @Override
    public int getId() {
        return id;
    }

    @NonNull @Override public Namespace getNamespace(@NonNull WikiSite wiki) {
        return guessNamespace(wiki, defaultString(normalizedtitle));
    }

    @Override
    public long getRevision() {
        return revision;
    }

    @Override
    @Nullable
    public String getLastModified() {
        return lastmodified;
    }

    @Override
    @Nullable
    public String getTitlePronunciationUrl() {
        return titlePronunciation == null
                ? null
                : UriUtil.resolveProtocolRelativeUrl(titlePronunciation.getUrl());
    }

    @Override
    @Nullable
    public Location getGeo() {
        return geo;
    }

    @Override
    public int getLanguageCount() {
        return languagecount;
    }

    @Override
    @Nullable
    public String getDisplayTitle() {
        return displaytitle;
    }

    @Override
    @Nullable
    public String getRedirected() {
        return redirected;
    }

    @Override
    @Nullable
    public String getNormalizedTitle() {
        return normalizedtitle;
    }

    @Override
    @Nullable
    public String getWikiBaseItem() {
        return wikiBaseItem;
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    @Nullable
    public String getLeadImageUrl(int leadImageWidth) {
        return image != null ? image.getUrl(leadImageWidth) : null;
    }

    @Override
    @Nullable
    public String getThumbUrl() {
        return image != null ? image.getUrl(PREFERRED_THUMB_SIZE) : null;
    }

    @Override
    @Nullable
    public String getLeadImageFileName() {
        return image != null ? image.getFileName() : null;
    }

    @Override
    @Nullable
    public String getFirstAllowedEditorRole() {
        return protection != null ? protection.getFirstAllowedEditorRole() : null;
    }

    @Override
    public boolean isEditable() {
        return editable || isLoggedInUserAllowedToEdit();
    }

    private boolean isLoggedInUserAllowedToEdit() {
        return protection != null && AccountUtil.isMemberOf(protection.getEditRoles());
    }

    @Override
    public boolean isMainPage() {
        return mainpage;
    }

    @Override
    public boolean isDisambiguation() {
        return disambiguation;
    }

    @Override @NonNull public List<Section> getSections() {
        return sections == null ? Collections.<Section>emptyList() : sections;
    }

    // TODO: remove this method and #getNamespace() WikiSite dependency when T135141 is fixed.
    @NonNull private Namespace guessNamespace(@NonNull WikiSite wiki, @NonNull String title) {
        String[] parts = title.split(":", -1);
        String name = parts.length > 1  ? parts[0] : null;
        return Namespace.fromLegacyString(wiki, name);
    }

    /**
     * For the lead image File: page name
     */
    public static class TitlePronunciation {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String url;

        @NonNull
        public String getUrl() {
            return url;
        }
    }

    /**
     * For the lead image File: page name
     */
    public static class Image {
        @SuppressWarnings("unused") @SerializedName("file") private String fileName;
        @SuppressWarnings("unused") private ThumbUrls urls;

        public String getFileName() {
            return fileName;
        }

        @Nullable
        public String getUrl(int width) {
            return urls != null ? urls.get(width) : null;
        }
    }

    /**
     * For the lead image URLs
     */
    public static class ThumbUrls {
        private static final int SMALL = 320;
        private static final int MEDIUM = 640;
        private static final int LARGE = 800;
        private static final int XL = 1024;
        @SuppressWarnings("unused") @SerializedName("320") private String small;
        @SuppressWarnings("unused") @SerializedName("640") private String medium;
        @SuppressWarnings("unused") @SerializedName("800") private String large;
        @SuppressWarnings("unused") @SerializedName("1024") private String xl;

        @Nullable
        public String get(int width) {
            switch (width) {
                case SMALL:
                    return small;
                case MEDIUM:
                    return medium;
                case LARGE:
                    return large;
                case XL:
                    return xl;
                default:
                    return null;
            }
        }
    }
}
