package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.PluralsRes;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public class ContinueReadingCardView extends ListCardView<ContinueReadingCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public ContinueReadingCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull ContinueReadingCard card) {
        super.setCard(card);
        header(card);
    }

    private void header(@NonNull ContinueReadingCard card) {
        int age = (int) card.daysOld();
        @PluralsRes int subtitlePlural;
        String subtitle;
        if (age == 0) {
            subtitle = getResources().getString(R.string.view_continue_reading_card_subtitle_today);
        } else {
            subtitlePlural = R.plurals.view_continue_reading_card_subtitle;
            subtitle = getResources().getQuantityString(subtitlePlural, age, age);
        }
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(R.string.view_continue_reading_card_title)
                .setSubtitle(subtitle)
                .setImage(R.drawable.ic_arrow_forward_black_24dp)
                .setImageCircleColor(R.color.base30)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
        CardLargeHeaderView largeHeader = new CardLargeHeaderView(getContext())
                .setTitle(card.title())
                .setImage(card.image())
                .onClickListener(new CardClickListener());
        largeHeader(largeHeader);
    }

    private class CardClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onSelectPage(getCard(), new HistoryEntry(getCard().pageTitle(),
                        HistoryEntry.SOURCE_FEED_CONTINUE_READING));
            }
        }
    }
}
