package org.wikipedia.page;

import androidx.annotation.NonNull;

import org.wikipedia.history.HistoryEntry;

public class PageBackStackItem {
    @NonNull private PageTitle title;
    @NonNull private HistoryEntry historyEntry;

    private int scrollY;

    public PageBackStackItem(@NonNull PageTitle title, @NonNull HistoryEntry historyEntry) {

        // TODO: remove this crash probe upon fixing
        if (title == null || historyEntry == null) {
            throw new IllegalArgumentException("Nonnull parameter is in fact null.");
        }

        this.title = title;
        this.historyEntry = historyEntry;
    }

    @NonNull
    public PageTitle getTitle() {
        return title;
    }

    public void setTitle(@NonNull PageTitle title) {
        this.title = title;
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
