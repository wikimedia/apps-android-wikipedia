package org.wikipedia.feed.random;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.feed.featured.FeaturedArticleCardView;
import org.wikipedia.feed.view.CardFooterView;

public class RandomCardView extends FeaturedArticleCardView {
    public interface Callback {
        void onRandomClick(@NonNull RandomCardView view);
    }

    public RandomCardView(Context context) {
        super(context);
    }

    @Override
    public CardFooterView.Callback getFooterCallback() {
        return () -> {
            if (getCallback() != null && getCard() != null) {
                getCallback().onRandomClick(RandomCardView.this);
            }
        };
    }
}
