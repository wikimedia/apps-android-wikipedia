package org.wikipedia.feed.offline;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedCardView;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class OfflineCardView extends LinearLayout implements FeedCardView<Card> {
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

    @Override public void setCard(@NonNull Card card) { }
    @Override @Nullable public Card getCard() {
        return null;
    }
}
