package org.wikipedia.feed.offline;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.databinding.ViewCardOfflineBinding;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedCardView;

public class OfflineCardView extends LinearLayout implements FeedCardView<Card> {
    private Space padding;

    @Nullable private FeedAdapter.Callback callback;

    public OfflineCardView(Context context) {
        super(context);

        final ViewCardOfflineBinding binding = ViewCardOfflineBinding.bind(this);

        padding = binding.viewCardOfflineTopPadding;
        binding.viewCardOfflineButtonRetry.setOnClickListener(v -> {
            if (callback != null) {
                callback.onRetryFromOffline();
            }
        });
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
