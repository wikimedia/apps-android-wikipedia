package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import org.wikipedia.feed.FeedViewCallback;

public class FeedCardView extends CardView {
    @Nullable private FeedViewCallback callback;

    public FeedCardView(Context context) {
        super(context);
    }

    public FeedCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FeedCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @NonNull public FeedCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    @Nullable protected FeedViewCallback getCallback() {
        return callback;
    }
}