package org.wikipedia.page;

import android.support.annotation.Nullable;

import org.wikipedia.history.HistoryEntry;

/**
 * Shared data between PageFragment and PageLoadStrategy
 */
class PageViewModel {
    @Nullable private Page page;
    private PageTitle title;
    private PageTitle titleOriginal;
    private HistoryEntry curEntry;

    @Nullable public Page getPage() {
        return page;
    }

    public void setPage(@Nullable Page page) {
        this.page = page;
    }

    public PageTitle getTitle() {
        return title;
    }

    public void setTitle(PageTitle title) {
        this.title = title;
    }

    public PageTitle getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(PageTitle titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public HistoryEntry getCurEntry() {
        return curEntry;
    }

    public void setCurEntry(HistoryEntry curEntry) {
        this.curEntry = curEntry;
    }
}
