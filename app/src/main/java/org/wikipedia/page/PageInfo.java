package org.wikipedia.page;

import android.support.annotation.NonNull;

/**
 * Holds similar titles (disambiguations) and content issues for a page.
 */
public class PageInfo {
    @NonNull private final DisambigResult[] similarTitles;
    @NonNull private final String[] contentIssues;
    @NonNull private final PageTitle title;

    public PageInfo(@NonNull PageTitle title,
                    @NonNull DisambigResult[] similarTitles,
                    @NonNull String[] contentIssues) {
        this.title = title;
        this.similarTitles = similarTitles;
        this.contentIssues = contentIssues;
    }

    @NonNull
    public PageTitle getTitle() {
        return title;
    }

    public boolean hasSimilarTitles() {
        return getSimilarTitles().length > 0;
    }

    @NonNull
    public DisambigResult[] getSimilarTitles() {
        return similarTitles;
    }

    public boolean hasContentIssues() {
        return getContentIssues().length > 0;
    }

    @NonNull
    public String[] getContentIssues() {
        return contentIssues;
    }

    @Override
    public String toString() {
        return title.getDisplayText() + ": "
                + contentIssues.length + " issue(s); "
                + similarTitles.length + " disambiguation(s)";
    }
}
