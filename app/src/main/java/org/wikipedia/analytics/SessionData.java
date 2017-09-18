package org.wikipedia.analytics;

import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.MathUtil;

public class SessionData {
    private final MathUtil.Averaged<Long> leadLatency = new MathUtil.Averaged<>();
    private final MathUtil.Averaged<Long> restLatency = new MathUtil.Averaged<>();
    private long startTime;
    private long lastTouchTime;
    private int pagesFromSearch;
    private int pagesFromRandom;
    private int pagesFromLangLink;
    private int pagesFromInternal;
    private int pagesFromExternal;
    private int pagesFromHistory;
    private int pagesFromReadingList;
    private int pagesFromNearby;
    private int pagesFromDisambig;
    private int pagesFromBack;
    private int pagesWithNoDescription;

    public void addPageViewed(HistoryEntry entry) {
        switch (entry.getSource()) {
            case HistoryEntry.SOURCE_SEARCH:
                pagesFromSearch++;
                break;
            case HistoryEntry.SOURCE_RANDOM:
                pagesFromRandom++;
                break;
            case HistoryEntry.SOURCE_LANGUAGE_LINK:
                pagesFromLangLink++;
                break;
            case HistoryEntry.SOURCE_EXTERNAL_LINK:
                pagesFromExternal++;
                break;
            case HistoryEntry.SOURCE_HISTORY:
                pagesFromHistory++;
                break;
            case HistoryEntry.SOURCE_READING_LIST:
                pagesFromReadingList++;
                break;
            case HistoryEntry.SOURCE_NEARBY:
                pagesFromNearby++;
                break;
            case HistoryEntry.SOURCE_DISAMBIG:
                pagesFromDisambig++;
                break;
            default:
                pagesFromInternal++;
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getLastTouchTime() {
        return lastTouchTime;
    }

    public void setLastTouchTime(long lastTouchTime) {
        this.lastTouchTime = lastTouchTime;
    }

    public long getLeadLatency() {
        return (long) leadLatency.getAverage();
    }

    public void addLeadLatency(long leadLatency) {
        this.leadLatency.addSample(leadLatency);
    }

    public long getRestLatency() {
        return (long) restLatency.getAverage();
    }

    public void addRestLatency(long restLatency) {
        this.restLatency.addSample(restLatency);
    }

    public int getPagesFromSearch() {
        return pagesFromSearch;
    }

    public int getPagesFromRandom() {
        return pagesFromRandom;
    }

    public int getPagesFromLangLink() {
        return pagesFromLangLink;
    }

    public int getPagesFromInternal() {
        return pagesFromInternal;
    }

    public int getPagesFromExternal() {
        return pagesFromExternal;
    }

    public int getPagesFromHistory() {
        return pagesFromHistory;
    }

    public int getPagesFromReadingList() {
        return pagesFromReadingList;
    }

    public int getPagesFromNearby() {
        return pagesFromNearby;
    }

    public int getPagesFromDisambig() {
        return pagesFromDisambig;
    }

    public int getPagesFromBack() {
        return pagesFromBack;
    }

    public int getPagesWithNoDescription() {
        return pagesWithNoDescription;
    }

    public void addPageFromBack() {
        pagesFromBack++;
    }

    public void addPageWithNoDescription() {
        pagesWithNoDescription++;
    }

    public int getTotalPages() {
        return pagesFromSearch + pagesFromRandom + pagesFromLangLink + pagesFromInternal
                + pagesFromExternal + pagesFromHistory + pagesFromReadingList + pagesFromNearby
                + pagesFromDisambig;
    }
}
