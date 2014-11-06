package org.wikipedia.page;

/**
 *
 */
public class PageInfo {
    private final String[] disambigs;
    private final String[] issues;

    public PageInfo(String[] disambigs, String[] issues) {
        this.disambigs = disambigs;
        this.issues = issues;
    }

    public String[] getDisambigs() {
        return disambigs;
    }

    public String[] getIssues() {
        return issues;
    }
}
