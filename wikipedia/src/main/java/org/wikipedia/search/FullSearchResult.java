package org.wikipedia.search;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public class FullSearchResult {
    private PageTitle title;
    private String snippet;
    private String redirectTitle;
    private String redirectSnippet;

    public PageTitle getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getRedirectTitle() {
        return redirectTitle;
    }

    public String getRedirectSnippet() {
        return redirectSnippet;
    }

    public FullSearchResult(String title, String snippet, String redirectTitle, String redirectSnippet) {
        this.title = new PageTitle(title, WikipediaApp.getInstance().getPrimarySite());
        this.snippet = transformHighlight(snippet);
        this.redirectTitle = redirectTitle;
        this.redirectSnippet = transformHighlight(redirectSnippet);
    }

    private String transformHighlight(String str) {
        final int colorMask = 0xFFFFFF;
        return str.replace("<span class='searchmatch'>", "<font color='"
                + String.format("#%06X", colorMask & WikipediaApp.getInstance().getResources().getColor(R.color.fulltext_search_highlight))
                + "'><strong>").replace("</span>", "</strong></font>");
    }
}
