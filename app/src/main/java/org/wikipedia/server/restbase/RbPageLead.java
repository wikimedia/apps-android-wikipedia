package org.wikipedia.server.restbase;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.wikipedia.page.GeoTypeAdapter;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageLeadProperties;
import org.wikipedia.server.Protection;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.util.List;

import static org.wikipedia.util.StringUtil.capitalizeFirstChar;

/**
 * Gson POJO for loading the first stage of page content.
 */
public class RbPageLead implements PageLead, PageLeadProperties {
    @Expose private RbServiceError error;
    @Expose private int id;
    @Expose private long revision;
    @Expose @Nullable private String lastmodified;
    @Expose @Nullable private String displaytitle;
    @Expose @Nullable private String redirected;
    @Expose @Nullable private String normalizedtitle;
    @Expose @Nullable @SerializedName("pronunciation") private TitlePronunciation titlePronunciation;
    @Expose @Nullable @JsonAdapter(GeoTypeAdapter.class) private Location geo;
    @Expose private int languagecount;
    @Expose private boolean editable;
    @Expose private boolean mainpage;
    @Expose private boolean disambiguation;
    @Expose @Nullable private String description;
    @Expose @Nullable private Image image;
    @Expose @Nullable private Protection protection;
    @Expose @Nullable private List<Section> sections;

    private transient int leadImageThumbWidth;

    @Override
    public boolean hasError() {
        return error != null || sections == null;
    }

    @Nullable
    public RbServiceError getError() {
        return error;
    }

    public void logError(String message) {
        if (error != null) {
            message += ": " + error.toString();
        }
        L.e(message);
    }

    /** Note: before using this check that #getMobileview != null */
    @Override
    public Page toPage(PageTitle title) {
        return new Page(adjustPageTitle(title),
                getSections(),
                toPageProperties());
    }

    /* package */ PageTitle adjustPageTitle(PageTitle title) {
        if (redirected != null) {
            // Handle redirects properly.
            title = new PageTitle(redirected, title.getSite(), title.getThumbUrl());
        } else if (normalizedtitle != null) {
            // We care about the normalized title only if we were not redirected
            title = new PageTitle(normalizedtitle, title.getSite(), title.getThumbUrl());
        }
        title.setDescription(description);
        return title;
    }

    public String getLeadSectionContent() {
        if (sections != null) {
            return sections.get(0).getContent();
        } else {
            return "";
        }
    }

    /** Converter */
    public PageProperties toPageProperties() {
        return new PageProperties(this);
    }

    public int getId() {
        return id;
    }

    public long getRevision() {
        return revision;
    }

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

    public int getLanguageCount() {
        return languagecount;
    }

    @Nullable
    public String getDisplayTitle() {
        return displaytitle;
    }

    @Nullable
    public String getRedirected() {
        return redirected;
    }

    @Nullable
    public String getNormalizedTitle() {
        return normalizedtitle;
    }

    @Nullable
    public String getDescription() {
        return description != null ? capitalizeFirstChar(description) : null;
    }

    @Nullable
    public String getLeadImageUrl() {
        return image != null ? image.getUrl(leadImageThumbWidth) : null;
    }

    @Nullable
    public String getLeadImageName() {
        return image != null ? image.getFile() : null;
    }

    @Nullable
    public String getFirstAllowedEditorRole() {
        return protection != null ? protection.getFirstAllowedEditorRole() : null;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isMainPage() {
        return mainpage;
    }

    public boolean isDisambiguation() {
        return disambiguation;
    }

    @Nullable
    public List<Section> getSections() {
        return sections;
    }

    public void setLeadImageThumbWidth(int leadImageThumbWidth) {
        this.leadImageThumbWidth = leadImageThumbWidth;
    }

    /**
     * For the lead image File: page name
     */
    public static class TitlePronunciation {
        @Expose @NonNull private String url;

        @NonNull
        public String getUrl() {
            return url;
        }
    }

    /**
     * For the lead image File: page name
     */
    public static class Image {
        @Expose private String file;
        @Expose private ThumbUrls urls;

        public String getFile() {
            return file;
        }

        @Nullable
        public String getUrl(int leadImageThumbWidth) {
            return urls != null ? urls.get(leadImageThumbWidth) : null;
        }
    }

    /**
     * For the lead image URLs
     */
    public static class ThumbUrls {
        private static final int SMALL = 640;
        private static final int MEDIUM = 800;
        private static final int LARGE = 1024;
        @Expose @SerializedName("640") private String small;
        @Expose @SerializedName("800") private String medium;
        @Expose @SerializedName("1024") private String large;

        @Nullable
        public String get(int leadImageThumbWidth) {
            switch (leadImageThumbWidth) {
                case SMALL:
                    return small;
                case MEDIUM:
                    return medium;
                case LARGE:
                    return large;
                default:
                    return null;
            }
        }
    }
}
