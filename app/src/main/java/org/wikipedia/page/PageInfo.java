package org.wikipedia.page;

/**
 * Holds information about disambigs and page issues for a page.
 */
public class PageInfo {
    private final DisambigResult[] disambigs;
    private final String[] issues;
    private final PageTitle title;

    public PageInfo(PageTitle title, DisambigResult[] disambigs, String[] issues) {
        this.title = title;
        this.disambigs = disambigs;
        this.issues = issues;
    }

    public PageTitle getTitle() {
        return title;
    }

    public DisambigResult[] getDisambigs() {
        return disambigs;
    }

    public String[] getIssues() {
        return issues;
    }
}
