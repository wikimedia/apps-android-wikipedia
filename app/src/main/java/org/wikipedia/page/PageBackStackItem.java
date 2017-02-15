package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.history.HistoryEntry;
import org.wikipedia.model.BaseModel;

public class PageBackStackItem extends BaseModel {
    private final PageTitle title;
    @NonNull private final HistoryEntry historyEntry;
    private int scrollY;

    public PageBackStackItem(PageTitle title, @NonNull HistoryEntry historyEntry) {
        this.title = title;
        this.historyEntry = historyEntry;
    }

    public PageTitle getTitle() {
        return title;
    }

    @NonNull public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }
}
