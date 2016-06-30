package org.wikipedia.feed;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public interface FeedViewCallback extends ItemTouchHelperSwipeAdapter.Callback {
    void onRequestMore();
    void onSelectPage(@NonNull HistoryEntry entry);
    void onAddPageToList(@NonNull HistoryEntry entry);
    void onSharePage(@NonNull HistoryEntry entry);
    void onSearchRequested();
    void onVoiceSearchRequested();
    boolean onRequestDismissCard(@NonNull Card card);
}