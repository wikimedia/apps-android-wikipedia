package org.wikipedia.feed.accessibility;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.databinding.ViewCardAccessibilityBinding;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedCardView;

public class AccessibilityCardView extends LinearLayout implements FeedCardView<Card> {
    @Nullable private FeedAdapter.Callback callback;

    public AccessibilityCardView(Context context) {
        super(context);
        final ViewCardAccessibilityBinding binding =
                ViewCardAccessibilityBinding.inflate(LayoutInflater.from(context));
        binding.viewCardAccessibilityButtonLoadMore.setOnClickListener(v -> {
            if (callback != null) {
                callback.onRequestMore();
            }
        });
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        this.callback = callback;
    }

    @Override public void setCard(@NonNull Card card) { }
    @Override @Nullable public Card getCard() {
        return null;
    }
}
