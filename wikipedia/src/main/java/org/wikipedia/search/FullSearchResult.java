package org.wikipedia.search;

import org.wikipedia.PageTitle;

public class FullSearchResult {
    private final PageTitle title;
    private final String thumbUrl;
    private final String wikiBaseId;

    public FullSearchResult(PageTitle title, String thumbUrl, String wikiBaseId) {
        this.thumbUrl = thumbUrl;
        this.wikiBaseId = wikiBaseId;
        this.title = title;
    }

    public PageTitle getTitle() {
        return title;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String getWikiBaseId() {
        return wikiBaseId;
    }
}
