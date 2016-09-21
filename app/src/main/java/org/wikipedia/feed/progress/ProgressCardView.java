package org.wikipedia.feed.progress;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.FrameLayout;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedCardView;
import org.wikipedia.feed.view.FeedViewCallback;

public class ProgressCardView extends FrameLayout implements FeedCardView<Card> {
    public ProgressCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_progress, this);
    }

    @Override public void setCard(@NonNull Card card) { }
    @Override public void setCallback(@Nullable FeedViewCallback callback) { }
}