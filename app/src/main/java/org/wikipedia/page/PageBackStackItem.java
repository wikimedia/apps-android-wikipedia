package org.wikipedia.page;

import org.wikipedia.history.HistoryEntry;

public class PageBackStackItem {
    private final PageTitle title;
    public PageTitle getTitle() {
        return title;
    }

    private final HistoryEntry historyEntry;
    public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    private int scrollY;
    public int getScrollY() {
        return scrollY;
    }
    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    public PageBackStackItem(PageTitle title, HistoryEntry historyEntry) {
        this.title = title;
        this.historyEntry = historyEntry;
    }
}