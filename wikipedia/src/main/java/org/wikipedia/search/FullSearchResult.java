package org.wikipedia.search;

import org.wikipedia.PageTitle;

public class FullSearchResult {
    private final PageTitle title;
    private final String wikiBaseId;

    public FullSearchResult(PageTitle title, String wikiBaseId) {
        this.wikiBaseId = wikiBaseId;
        this.title = title;
    }

    public PageTitle getTitle() {
        return title;
    }

    public String getWikiBaseId() {
        return wikiBaseId;
    }
}
