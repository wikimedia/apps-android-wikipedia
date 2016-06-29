package org.wikipedia.feed;

import android.support.annotation.NonNull;

import org.wikipedia.history.HistoryEntry;

public interface FeedViewCallback {
    void onRequestMore();
    void onSelectPage(@NonNull HistoryEntry entry);
    void onAddPageToList(@NonNull HistoryEntry entry);
    void onSharePage(@NonNull HistoryEntry entry);
    void onSearchRequested();
    void onVoiceSearchRequested();
}
