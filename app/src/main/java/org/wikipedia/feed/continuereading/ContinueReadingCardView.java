package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.PluralsRes;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.ListCardView;

public class ContinueReadingCardView extends ListCardView<ContinueReadingCard> {
    public ContinueReadingCardView(Context context) {
        super(context);
    }

    public void set(@NonNull ContinueReadingCard card) {
        header(card);
    }

    private void header(@NonNull ContinueReadingCard card) {
        @PluralsRes int subtitle = R.plurals.view_continue_reading_card_subtitle;
        int age = (int) card.daysOld();
        CardLargeHeaderView header = new CardLargeHeaderView(getContext())
                .setTitle(R.string.view_continue_reading_card_title)
                .setSubtitle(getResources().getQuantityString(subtitle, age, age))
                .setPageTitle(card.title())
                .setImage(card.image());
        header(header);
    }
}