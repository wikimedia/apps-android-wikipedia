package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.ListCardView;

public class ContinueReadingCardView extends ListCardView<ContinueReadingCard> {
    private ContinueReadingCard card;
    @Nullable private FeedViewCallback callback;

    public ContinueReadingCardView(Context context) {
        super(context);
    }

    @NonNull public ContinueReadingCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    public void set(@NonNull ContinueReadingCard card) {
        this.card = card;
        header(card);
    }

    private void header(@NonNull ContinueReadingCard card) {
        @PluralsRes int subtitle = R.plurals.view_continue_reading_card_subtitle;
        int age = (int) card.daysOld();
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(R.string.view_continue_reading_card_title)
                .setSubtitle(getResources().getQuantityString(subtitle, age, age))
                .setImage(R.drawable.ic_arrow_forward_black_24dp)
                .setImageCircleColor(R.color.gray_highlight);
        header(header);
        CardLargeHeaderView largeHeader = new CardLargeHeaderView(getContext())
                .setPageTitle(card.title())
                .setImage(card.image())
                .onClickListener(new CardClickListener());
        largeHeader(largeHeader);
    }

    private class CardClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (callback != null) {
                callback.onSelectPage(card.pageTitle());
            }
        }
    }
}