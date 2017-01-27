package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.history.HistoryEntry;
import org.wikipedia.model.BaseModel;

public class PageBackStackItem extends BaseModel {
    private final PageTitle title;
    public PageTitle getTitle() {
        return title;
    }

    @NonNull private final HistoryEntry historyEntry;
    @NonNull public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    private int scrollY;
    public int getScrollY() {
        return scrollY;
    }
    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    public PageBackStackItem(PageTitle title, @NonNull HistoryEntry historyEntry) {
        this.title = title;
        this.historyEntry = historyEntry;
    }
}