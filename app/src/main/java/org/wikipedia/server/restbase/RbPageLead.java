package org.wikipedia.server.restbase;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.wikipedia.Site;
import org.wikipedia.page.GeoTypeAdapter;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageLeadProperties;
import org.wikipedia.server.Protection;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.util.List;

import static org.wikipedia.util.StringUtil.capitalizeFirstChar;

/**
 * Gson POJO for loading the first stage of page content.
 */
public class RbPageLead implements PageLead, PageLeadProperties {
    private RbServiceError error;
    private int id;
    private long revision;
    @Nullable private String lastmodified;
    @Nullable private String displaytitle;
    @Nullable private String redirected;
    @Nullable private String normalizedtitle;
    @Nullable @SerializedName("pronunciation") private TitlePronunciation titlePronunciation;
    @Nullable @JsonAdapter(GeoTypeAdapter.class) private Location geo;
    private int languagecount;
    private boolean editable;
    private boolean mainpage;
    private boolean disambiguation;
    @Nullable private String description;
    @Nullable private Image image;
    @Nullable private Protection protection;
    @Nullable private List<Section> sections;

    private transient int leadImageThumbWidth;

    @Override
    public boolean hasError() {
        return error != null || sections == null;
    }

    @Override
    @Nullable
    public RbServiceError getError() {
        return error;
    }

    @Override
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
                toPageProperties(title.getSite()));
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

    @Override
    public String getLeadSectionContent() {
        if (sections != null) {
            return sections.get(0).getContent();
        } else {
            return "";
        }
    }

    /** Converter */
    public PageProperties toPageProperties(@NonNull Site site) {
        return new PageProperties(site, this);
    }

    @Override
    public int getId() {
        return id;
    }

    @NonNull @Override public Namespace getNamespace(@NonNull Site site) {
        return guessNamespace(site, StringUtil.emptyIfNull(normalizedtitle));
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
    public String getDescription() {
        return description != null ? capitalizeFirstChar(description) : null;
    }

    @Override
    @Nullable
    public String getLeadImageUrl() {
        return image != null ? image.getUrl(leadImageThumbWidth) : null;
    }

    @Override
    @Nullable
    public String getLeadImageName() {
        return image != null ? image.getFile() : null;
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

    @Override
    @Nullable
    public List<Section> getSections() {
        return sections;
    }

    public void setLeadImageThumbWidth(int leadImageThumbWidth) {
        this.leadImageThumbWidth = leadImageThumbWidth;
    }

    // TODO: remove this method and #getNamespace() Site dependency when T135141 is fixed.
    @NonNull private Namespace guessNamespace(@NonNull Site site, @NonNull String title) {
        String[] parts = title.split(":", -1);
        String name = parts.length > 1  ? parts[0] : null;
        return Namespace.fromLegacyString(site, name);
    }

    /**
     * For the lead image File: page name
     */
    public static class TitlePronunciation {
        @NonNull private String url;

        @NonNull
        public String getUrl() {
            return url;
        }
    }

    /**
     * For the lead image File: page name
     */
    public static class Image {
        private String file;
        private ThumbUrls urls;

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
        @SerializedName("640") private String small;
        @SerializedName("800") private String medium;
        @SerializedName("1024") private String large;

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
