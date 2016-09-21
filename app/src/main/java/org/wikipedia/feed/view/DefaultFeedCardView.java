package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;

import org.wikipedia.feed.model.Card;

public abstract class DefaultFeedCardView<T extends Card> extends CardView implements FeedCardView<T> {
    @Nullable private T card;
    @Nullable private FeedAdapter.Callback callback;

    public DefaultFeedCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull T card) {
        this.card = card;
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    @Nullable protected T getCard() {
        return card;
    }

    @Nullable protected FeedAdapter.Callback getCallback() {
        return callback;
    }
}