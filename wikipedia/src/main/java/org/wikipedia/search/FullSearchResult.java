package org.wikipedia.search;

import org.wikipedia.PageTitle;

public class FullSearchResult {
    private final PageTitle title;
    private final String thumbUrl;
    private final String description;

    public FullSearchResult(PageTitle title, String thumbUrl, String description) {
        this.thumbUrl = thumbUrl;
        this.description = description;
        this.title = title;
    }

    public PageTitle getTitle() {
        return title;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String getDescription() {
        return description;
    }
}
