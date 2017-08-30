package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.offline.Compilation;
import org.wikipedia.settings.RbSwitch;

import java.util.Date;
import java.util.List;

/**
 * Represents a particular page along with its full contents.
 */
public class Page {
    @VisibleForTesting static final int MEDIAWIKI_ORIGIN = 0;
    @VisibleForTesting static final int RESTBASE_ORIGIN = 1;

    @NonNull private final PageTitle title;
    @NonNull private final List<Section> sections;
    @NonNull private final PageProperties pageProperties;

    @Nullable private String compName;
    @Nullable private Date compDate;

    /**
     * An indicator what payload version the page content was originally retrieved from.
     * If it's set to RESTBASE_ORIGIN the it came from the Mobile Content Service
     * (via RESTBase). This is esp. useful for saved pages, so that an older saved page will get the
     * correct kind of DOM transformations applied.
     */
    private int version = MEDIAWIKI_ORIGIN;

    /** Regular constructor */
    public Page(@NonNull PageTitle title, @NonNull List<Section> sections,
                @NonNull PageProperties pageProperties) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled(title.getWikiSite())) {
            this.version = RESTBASE_ORIGIN;
        }
        this.title = title;
        this.sections = sections;
        this.pageProperties = pageProperties;
    }

    @VisibleForTesting Page(@NonNull PageTitle title, @NonNull List<Section> sections,
         @NonNull PageProperties pageProperties, int version) {
        this.version = version;
        this.title = title;
        this.sections = sections;
        this.pageProperties = pageProperties;
    }

    /**
     * This could also be called getVersion but since there are only two different versions
     * I like to call it isFromRestBase to make it clearer.
     */
    public boolean isFromRestBase() {
        return version == RESTBASE_ORIGIN;
    }

    @NonNull public PageTitle getTitle() {
        return title;
    }

    @NonNull public List<Section> getSections() {
        return sections;
    }

    public String getDisplayTitle() {
        return pageProperties.getDisplayTitle();
    }

    @Nullable public String getTitlePronunciationUrl() {
        return getPageProperties().getTitlePronunciationUrl();
    }

    @NonNull public PageProperties getPageProperties() {
        return pageProperties;
    }

    public boolean couldHaveReadMoreSection() {
        return getTitle().namespace() == Namespace.MAIN;
    }

    public boolean isFilePage() {
        return title.isFilePage();
    }

    public boolean isMainPage() {
        return pageProperties.isMainPage();
    }

    public boolean isArticle() {
        return !isMainPage() && getTitle().namespace() == Namespace.MAIN;
    }

    public boolean isFromOfflineCompilation() {
        return compName != null;
    }

    @Nullable public Date getCompilationDate() {
        return compDate;
    }

    @Nullable public String getCompilationName() {
        return compName;
    }

    public void setCompilation(@NonNull Compilation comp) {
        this.compName = comp.name();
        this.compDate = comp.date();
    }

    /** For old PHP API */
    public void addRemainingSections(List<Section> remainingSections) {
        sections.addAll(remainingSections);
    }

    /** For new RESTBase API */
    public void augmentRemainingSections(List<Section> remainingSections) {
        // TODO: Use Parsoid to request the same revision ID, so that there's no race condition
        // that can lead to a mismatched number of sections.
        Section leadSection = sections.get(0);
        sections.clear();
        sections.add(leadSection);
        sections.addAll(remainingSections);
    }

    public boolean isProtected() {
        return !getPageProperties().canEdit();
    }
}
