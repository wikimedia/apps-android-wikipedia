package org.wikipedia.feed.view;

import org.wikipedia.feed.model.Card;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface FeedCardView<T extends Card> {
    void setCard(@NonNull T card);
    @Nullable T getCard();
    void setCallback(@Nullable FeedAdapter.Callback callback);
}
