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

public class ContinueReadingCardView extends ListCardView
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    private ContinueReadingCard card;

    public ContinueReadingCardView(Context context) {
        super(context);
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
                .setImageCircleColor(R.color.gray_highlight)
                .setCard(card)
                .setCallback(getCallback());
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
            if (getCallback() != null) {
                getCallback().onSelectPage(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_FEED_CONTINUE_READING));
            }
        }
    }
}
