package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardRecyclerAdapter;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.DateUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import java.util.List;

public class BecauseYouReadCardView extends ListCardView<BecauseYouReadCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onSelectPageFromExistingTab(@NonNull Card card, @NonNull HistoryEntry entry);
    }

    public BecauseYouReadCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull BecauseYouReadCard card) {
        super.setCard(card);
        header(card);
        set(new RecyclerAdapter(card.items()));
    }

    private void header(@NonNull final BecauseYouReadCard card) {
        int age = (int) card.daysOld();
        headerView().setTitle(card.title())
                .setImage(R.drawable.ic_restore_black_24dp)
                .setImageCircleColor(R.color.base30)
                .setLangCode(null)
                .setCard(card)
                .setCallback(getCallback());
        largeHeaderView().setTitle(card.pageTitle())
                .setImage(card.image())
                .setSubtitle(DateUtil.getDaysAgoString(age))
                .onClickListener(new SelectPageCallbackAdapter(card))
                .setVisibility(VISIBLE);
    }

    private class SelectPageCallbackAdapter implements OnClickListener {
        @NonNull private final BecauseYouReadCard card;

        SelectPageCallbackAdapter(@NonNull BecauseYouReadCard card) {
            this.card = card;
        }

        @Override public void onClick(View view) {
            if (getCallback() != null) {
                getCallback().onSelectPageFromExistingTab(card, new HistoryEntry(card.getPageTitle(),
                        HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ));
            }
        }
    }

    private class RecyclerAdapter extends ListCardRecyclerAdapter<BecauseYouReadItemCard> {
        RecyclerAdapter(@NonNull List<BecauseYouReadItemCard> items) {
            super(items);
        }

        @Nullable @Override protected ListCardItemView.Callback callback() {
            return getCallback();
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<ListCardItemView> holder, int i) {
            BecauseYouReadItemCard card = item(i);
            holder.getView().setCard(card)
                    .setHistoryEntry(new HistoryEntry(card.pageTitle(),
                            HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ));
        }
    }
}
