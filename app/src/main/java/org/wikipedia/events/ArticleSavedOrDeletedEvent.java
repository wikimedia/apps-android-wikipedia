package org.wikipedia.events;

import org.wikipedia.readinglist.database.ReadingListPage;

public class ArticleSavedOrDeletedEvent {
    private ReadingListPage[] pages;

    public ReadingListPage[] getPages() {
        return pages;
    }

    public ArticleSavedOrDeletedEvent(ReadingListPage... pages) {
        this.pages = pages;
    }

}
