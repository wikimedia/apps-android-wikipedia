package org.wikipedia.page;

import org.wikipedia.history.HistoryEntry;

/**
 * Shared data between PageViewFragmentInternal and PageLoadStrategy
 */
class PageViewModel {
    private Page page;
    private PageTitle title;
    private PageTitle titleOriginal;
    private HistoryEntry curEntry;

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
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
