package org.wikipedia.events;

import org.wikipedia.readinglist.database.ReadingListPage;

public class PageDownloadEvent {
    private ReadingListPage page;

    public PageDownloadEvent(ReadingListPage page) {
        this.page = page;
    }

    public ReadingListPage getPage() {
        return page;
    }
}
