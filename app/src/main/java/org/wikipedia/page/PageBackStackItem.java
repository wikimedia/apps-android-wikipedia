package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.history.HistoryEntry;
import org.wikipedia.model.BaseModel;

public class PageBackStackItem extends BaseModel {
    @NonNull private PageTitle title;
    @NonNull private HistoryEntry historyEntry;

    private int scrollY;

    public PageBackStackItem(@NonNull PageTitle title, @NonNull HistoryEntry historyEntry) {
        this.title = title;
        this.historyEntry = historyEntry;
    }

    @NonNull
    public PageTitle getTitle() {
        return title;
    }

    @NonNull
    public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }
}
