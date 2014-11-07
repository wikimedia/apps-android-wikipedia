package org.wikipedia.page;

import org.wikipedia.PageTitle;

public class DisambigResult {
    private final PageTitle title;

    public DisambigResult(PageTitle title) {
        this.title = title;
    }

    public PageTitle getTitle() {
        return title;
    }
}
