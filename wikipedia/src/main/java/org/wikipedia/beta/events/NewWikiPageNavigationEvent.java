package org.wikipedia.beta.events;

import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.history.HistoryEntry;

public class NewWikiPageNavigationEvent {
    private final PageTitle title;
    private final HistoryEntry historyEntry;

    public NewWikiPageNavigationEvent(PageTitle title, HistoryEntry historyEntry) {
        this.title = title;
        this.historyEntry = historyEntry;
    }

    public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    public PageTitle getTitle() {
        return title;
    }
}