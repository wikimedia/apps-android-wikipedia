package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.PluralsRes;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.feed.view.PageTitleListCardItemView;
import org.wikipedia.feed.view.PageTitleRecyclerAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import java.util.List;

public class BecauseYouReadCardView extends ListCardView<BecauseYouReadCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {

    public BecauseYouReadCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull BecauseYouReadCard card) {
        super.setCard(card);
        header(card);
        set(new RecyclerAdapter(card.items()));
    }

    private void header(@NonNull final BecauseYouReadCard card) {
        @PluralsRes int subtitle = R.plurals.view_continue_reading_card_subtitle;
        int age = (int) card.daysOld();
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(getResources().getQuantityString(subtitle, age, age))
                .setImage(R.drawable.ic_restore_black_24dp)
                .setImageCircleColor(R.color.gray_highlight)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
        CardLargeHeaderView largeHeader = new CardLargeHeaderView(getContext())
                .setTitle(card.pageTitle())
                .setImage(card.image())
                .onClickListener(new SelectPageCallbackAdapter(card));
        largeHeader(largeHeader);
    }

    private class SelectPageCallbackAdapter implements OnClickListener {
        @NonNull private final BecauseYouReadCard card;

        SelectPageCallbackAdapter(@NonNull BecauseYouReadCard card) {
            this.card = card;
        }

        @Override public void onClick(View view) {
            if (getCallback() != null) {
                getCallback().onSelectPage(new HistoryEntry(card.getPageTitle(),
                        HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ));
            }
        }
    }

    private class RecyclerAdapter extends PageTitleRecyclerAdapter<BecauseYouReadItemCard> {
        RecyclerAdapter(@NonNull List<BecauseYouReadItemCard> items) {
            super(items);
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<PageTitleListCardItemView> holder, int i) {
            BecauseYouReadItemCard card = item(i);
            holder.getView().setHistoryEntry(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ));
            holder.getView().setCallback(getCallback());
        }
    }
}
