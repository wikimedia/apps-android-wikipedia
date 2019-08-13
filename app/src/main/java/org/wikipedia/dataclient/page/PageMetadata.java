package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class PageMetadata {
    private long revision;
    @Nullable private String tid;
    @Nullable private List<Hatnote> hatnotes;
    @Nullable private TableOfContents toc;
    @Nullable @SerializedName("language_links") private List<LanguageLink> languageLinks;
    @Nullable private List<Category> categories;
    @Nullable private Protection protection;
    @Nullable @SerializedName("description_source") private String descriptionSource;

    /** Converter */
    private PageProperties toPageProperties() {
        return new PageProperties(this);
    }

    public long getRevision() {
        return revision;
    }

    @NonNull public List<LanguageLink> getLanguageLinks() {
        return languageLinks != null ? languageLinks : Collections.emptyList();
    }

    @NonNull public String getDescriptionSource() {
        return StringUtils.defaultString(descriptionSource);
    }

    @Nullable public String getFirstAllowedEditorRole() {
        return protection != null ? protection.getFirstAllowedEditorRole() : null;
    }

    public boolean isEditable() {
        return isLoggedInUserAllowedToEdit();
    }

    private boolean isLoggedInUserAllowedToEdit() {
        return protection != null && AccountUtil.isMemberOf(protection.getEditRoles());
    }

    @NonNull public List<Section> getSections() {
        return toc != null ? toc.getEntries() : Collections.emptyList();
    }

    public static class TableOfContents {
        @Nullable private String title;
        @Nullable private List<Section> entries;

        @NonNull public List<Section> getEntries() {
            return entries != null ? entries : Collections.emptyList();
        }
    }

    public static class Section {
        private int level;
        private int section;
        @Nullable private String number;
        @Nullable private String anchor;
        @Nullable private String html;
    }

    public static class Hatnote {
        private int section;
        @Nullable private String html;

        public int getSection() {
            return section;
        }

        @NonNull public String getHtml() {
            return StringUtils.defaultString(html);
        }
    }

    public static class LanguageLink {
        @Nullable private String lang;
        @Nullable @SerializedName("summary_url") private String summaryUrl;
        @Nullable private Titles titles;
    }

    public static class Category {
        private int ns;
        private boolean hidden;
        @Nullable @SerializedName("summary_url") private String summaryUrl;
        @Nullable private Titles titles;
    }

    public static class Titles {
        @Nullable private String canonical;
        @Nullable private String normalized;
        @Nullable private String display;
    }
}
