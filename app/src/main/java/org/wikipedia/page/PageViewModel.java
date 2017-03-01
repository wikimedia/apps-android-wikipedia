package org.wikipedia.page;

import android.support.annotation.Nullable;

import org.wikipedia.history.HistoryEntry;

/**
 * Shared data between PageFragment and PageDataClient
 */
class PageViewModel {
    @Nullable private Page page;
    @Nullable private PageTitle title;
    @Nullable private PageTitle titleOriginal;
    @Nullable private HistoryEntry curEntry;

    @Nullable public Page getPage() {
        return page;
    }

    public void setPage(@Nullable Page page) {
        this.page = page;
    }

    @Nullable public PageTitle getTitle() {
        return title;
    }

    public void setTitle(@Nullable PageTitle title) {
        this.title = title;
    }

    @Nullable public PageTitle getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(@Nullable PageTitle titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    @Nullable public HistoryEntry getCurEntry() {
        return curEntry;
    }

    public void setCurEntry(@Nullable HistoryEntry curEntry) {
        this.curEntry = curEntry;
    }
}
