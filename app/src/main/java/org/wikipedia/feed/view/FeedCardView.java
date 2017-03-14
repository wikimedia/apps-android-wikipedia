package org.wikipedia.feed.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.model.Card;

public interface FeedCardView<T extends Card> {
    void setCard(@NonNull T card);
    @Nullable T getCard();
    void setCallback(@Nullable FeedAdapter.Callback callback);
}
