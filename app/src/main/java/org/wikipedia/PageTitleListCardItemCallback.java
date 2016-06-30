package org.wikipedia;

import android.support.annotation.NonNull;

import org.wikipedia.history.HistoryEntry;

public interface PageTitleListCardItemCallback {
    void onSelectPage(@NonNull HistoryEntry entry);
    void onAddPageToList(@NonNull HistoryEntry entry);
    void onSharePage(@NonNull HistoryEntry entry);
}
