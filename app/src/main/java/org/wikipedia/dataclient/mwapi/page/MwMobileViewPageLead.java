package org.wikipedia.dataclient.mwapi.page;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwResponse;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageLeadProperties;
import org.wikipedia.dataclient.page.Protection;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;

import java.util.Collections;
import java.util.List;

import static org.wikipedia.dataclient.Service.PREFERRED_THUMB_SIZE;
import static org.wikipedia.util.ImageUrlUtil.getUrlForSize;

/**
 * Gson POJO for loading the first stage of page content.
 */
public class MwMobileViewPageLead extends MwResponse implements PageLead {
    @SuppressWarnings("unused") private Mobileview mobileview;

    /** Note: before using this check that #getMobileview != null */
    @Override
    public Page toPage(@NonNull PageTitle title) {
        return new Page(adjustPageTitle(title, title.getPrefixedText()),
                mobileview.getSections(),
                mobileview.toPageProperties());
    }

    private PageTitle adjustPageTitle(@NonNull PageTitle title, @NonNull String originalPrefixedText) {
        if (mobileview.getRedirected() != null) {
            // Handle redirects properly.
            title = new PageTitle(mobileview.getRedirected(), title.getWikiSite(),
                    title.getThumbUrl());
        } else if (mobileview.getNormalizedTitle() != null) {
            // We care about the normalized title only if we were not redirected
            title = new PageTitle(mobileview.getNormalizedTitle(), title.getWikiSite(),
                    title.getThumbUrl());
        }

        if (mobileview.getDisplayTitle() != null
                && !StringUtil.removeHTMLTags(title.getDisplayText()).equals(StringUtil.removeHTMLTags(mobileview.getDisplayTitle()))) {
            title = new PageTitle(StringUtil.removeHTMLTags(mobileview.getDisplayTitle()), title.getWikiSite(),
                    title.getThumbUrl());
        }

        if (mobileview.getDisplayTitle() != null
                && !mobileview.getDisplayTitle().equals(originalPrefixedText)
                && mobileview.getNormalizedTitle() == null) {
            // Sometimes the MW api will not give us the "converted" or "redirected" title if switching between Chinese variants
            // Ticket: https://phabricator.wikimedia.org/T206891#4672777
            // We can the original prefixed title text (the one we used for calling API) to build the PageTitle
            title = new PageTitle(originalPrefixedText, title.getWikiSite(), title.getThumbUrl());
        }

        title.setDescription(mobileview.getDescription());
        return title;
    }

    @Override @NonNull public String getLeadSectionContent() {
        if (mobileview != null) {
            return mobileview.getSections().get(0).getContent();
        }
        return "";
    }

    @Nullable
    @Override
    public String getTitlePronunciationUrl() {
        return null;
    }

    @Nullable @Override public String getLeadImageUrl(int leadImageWidth) {
        return mobileview == null ? null : mobileview.getLeadImageUrl(leadImageWidth);
    }

    @Nullable @Override public String getThumbUrl() {
        return mobileview == null ? null : mobileview.getThumbUrl();
    }

    @Nullable @Override public String getDescription() {
        return mobileview == null ? null : mobileview.getDescription();
    }

    @Nullable
    @Override
    public Location getGeo() {
        return null;
    }

    @VisibleForTesting
    public Mobileview getMobileview() {
        return mobileview;
    }


    /**
     * Almost everything is in this inner class.
     */
    public static class Mobileview implements PageLeadProperties {
        @SuppressWarnings("unused") private int id;
        @SuppressWarnings("unused") private int namespace;
        @SuppressWarnings("unused") private long revision;
        @SuppressWarnings("unused") @Nullable private String lastmodified;
        @SuppressWarnings("unused") @Nullable private String displaytitle;
        @SuppressWarnings("unused") @Nullable private String redirected;
        @SuppressWarnings("unused") @Nullable private String normalizedtitle;
        @SuppressWarnings("unused") private int languagecount;
        @SuppressWarnings("unused") private boolean editable;
        @SuppressWarnings("unused") private boolean mainpage;
        @SuppressWarnings("unused") private boolean disambiguation;
        @SuppressWarnings("unused") @Nullable private String description;
        @SuppressWarnings("unused") @Nullable private String descriptionsource;
        @SuppressWarnings("unused") @SerializedName("image") @Nullable private PageImage pageImage;
        @SuppressWarnings("unused") @SerializedName("thumb") @Nullable private PageImageThumb leadImage;
        @SuppressWarnings("unused") @Nullable private Protection protection;
        @SuppressWarnings("unused") @Nullable private List<Section> sections;
        @SuppressWarnings("unused") @Nullable private MwQueryPage.PageProps pageprops;

        /** Converter */
        public PageProperties toPageProperties() {
            return new PageProperties(this);
        }

        @Override
        public int getId() {
            return id;
        }

        @Override @NonNull public Namespace getNamespace() {
            return Namespace.of(namespace);
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
        public String getTitlePronunciationUrl() {
            return null;
        }

        @Override
        @Nullable
        public Location getGeo() {
            return null;
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

        @Nullable
        public String getDescription() {
            return description;
        }

        @Override
        @Nullable
        public String getLeadImageUrl(int leadImageWidth) {
            return leadImage != null ? leadImage.getUrl() : null;
        }

        @Override
        @Nullable
        public String getThumbUrl() {
            return leadImage != null ? UriUtil.resolveProtocolRelativeUrl(getUrlForSize(leadImage.getUrl(), PREFERRED_THUMB_SIZE)) : null;
        }

        @Override
        @Nullable
        public String getLeadImageFileName() {
            return pageImage != null ? pageImage.getFileName() : null;
        }

        @Override
        @Nullable
        public String getWikiBaseItem() {
            return pageprops != null && pageprops.getWikiBaseItem() != null ? pageprops.getWikiBaseItem() : null;
        }

        @Override
        @Nullable
        public String getDescriptionSource() {
            return descriptionsource;
        }

        @Override
        @Nullable
        public String getFirstAllowedEditorRole() {
            return protection != null ? protection.getFirstAllowedEditorRole() : null;
        }

        @Override
        public boolean isEditable() {
            return editable;
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
            return sections == null ? Collections.emptyList() : sections;
        }
    }

    /**
     * For the lead image File: page name
     */
    public static class PageImage {
        @SuppressWarnings("unused") @SerializedName("file") private String fileName;

        public String getFileName() {
            return fileName;
        }
    }

    /**
     * For the lead image URL
     */
    public static class PageImageThumb {
        @SuppressWarnings("unused") private String url;

        public String getUrl() {
            return url;
        }
    }
}
