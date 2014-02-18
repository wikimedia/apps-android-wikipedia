package org.wikipedia.events;

import org.wikipedia.*;
import org.wikipedia.history.*;

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