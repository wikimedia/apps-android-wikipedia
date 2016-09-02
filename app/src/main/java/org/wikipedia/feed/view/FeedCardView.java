package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;

public class FeedCardView extends CardView {
    @Nullable private FeedViewCallback callback;

    public FeedCardView(Context context) {
        super(context);
    }

    @NonNull public FeedCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    @Nullable protected FeedViewCallback getCallback() {
        return callback;
    }
}