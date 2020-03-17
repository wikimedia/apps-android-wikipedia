package org.wikipedia.page;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a particular page along with its full contents.
 */
public class Page {
    @NonNull private final PageTitle title;
    @NonNull private List<Section> sections = new ArrayList<>();
    @NonNull private final PageProperties pageProperties;

    /** Regular constructor */
    public Page(@NonNull PageTitle title, @NonNull List<Section> sections,
                @NonNull PageProperties pageProperties) {
        this.title = title;
        this.sections = sections;
        this.pageProperties = pageProperties;
    }

    public Page(@NonNull PageTitle title, @NonNull PageProperties pageProperties) {
        this.title = title;
        this.pageProperties = pageProperties;
    }

    @NonNull public PageTitle getTitle() {
        return title;
    }

    @NonNull public List<Section> getSections() {
        return sections;
    }

    public void setSections(@NonNull List<Section> sections) {
        this.sections = sections;
    }

    public String getDisplayTitle() {
        return pageProperties.getDisplayTitle();
    }

    @NonNull public PageProperties getPageProperties() {
        return pageProperties;
    }

    public boolean isMainPage() {
        return pageProperties.isMainPage();
    }

    public boolean isArticle() {
        return !isMainPage() && getTitle().namespace() == Namespace.MAIN;
    }

    public boolean isProtected() {
        return !getPageProperties().canEdit();
    }
}
