package org.wikipedia.page;

/**
 * Holds information about disambigs and page issues for a page.
 */
public class PageInfo {
    private final DisambigResult[] disambigs;
    private final String[] issues;

    public PageInfo(DisambigResult[] disambigs, String[] issues) {
        this.disambigs = disambigs;
        this.issues = issues;
    }

    public DisambigResult[] getDisambigs() {
        return disambigs;
    }

    public String[] getIssues() {
        return issues;
    }
}
