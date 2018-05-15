package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.DateUtil;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

public class ContinueReadingCardView extends ListCardView<ContinueReadingCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onSelectPageFromExistingTab(@NonNull Card card, @NonNull HistoryEntry entry);
    }

    public ContinueReadingCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull ContinueReadingCard card) {
        super.setCard(card);
        header(card);
    }

    private void header(@NonNull ContinueReadingCard card) {
        int age = (int) card.daysOld();
        headerView().setTitle(R.string.view_continue_reading_card_title)
                .setImage(R.drawable.ic_arrow_forward_black_24dp)
                .setImageCircleColor(R.color.base30)
                .setLangCode(null)
                .setCard(card)
                .setCallback(getCallback());
        largeHeaderView().setTitle(card.title())
                .setImage(card.image())
                .setSubtitle(DateUtil.getDaysAgoString(age))
                .onClickListener(new CardClickListener())
                .setVisibility(VISIBLE);
    }

    private class CardClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (getCallback() != null && getCard() != null) {
                getCallback().onSelectPageFromExistingTab(getCard(), new HistoryEntry(getCard().pageTitle(),
                        HistoryEntry.SOURCE_FEED_CONTINUE_READING));
            }
        }
    }
}
