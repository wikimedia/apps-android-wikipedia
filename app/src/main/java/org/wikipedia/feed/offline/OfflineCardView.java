package org.wikipedia.feed.offline;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;
import android.widget.Space;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedCardView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OfflineCardView extends LinearLayout implements FeedCardView<Card> {
    @BindView(R.id.view_card_offline_top_padding) Space padding;

    @Nullable private FeedAdapter.Callback callback;

    public OfflineCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_offline, this);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.view_card_offline_button_retry) void onRetryClick() {
        if (callback != null) {
            callback.onRetryFromOffline();
        }
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    // This view has a transparent background, so it'll need a little padding if it appears directly
    // below the search card, so that it doesn't partially overlap the dark blue background.
    public void setTopPadding() {
        padding.setVisibility(VISIBLE);
    }

    // Hide the top padding when detached so that if this View is reused further down the feed, it
    // won't have the leftover padding inappropriately.
    @Override public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        padding.setVisibility(GONE);
    }

    @Override public void setCard(@NonNull Card card) { }
    @Override @Nullable public Card getCard() {
        return null;
    }
}
