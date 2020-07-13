package org.wikipedia.readinglist.sync;

public class ReadingListSyncEvent {
    private boolean showMessage;

    public ReadingListSyncEvent(boolean showMessage) {
        this.showMessage = showMessage;
    }

    public boolean showMessage() {
        return showMessage;
    }
}
