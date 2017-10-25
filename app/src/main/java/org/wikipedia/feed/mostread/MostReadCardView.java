package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardRecyclerAdapter;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import java.util.List;

public class MostReadCardView extends ListCardView<MostReadListCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    private static final int EVENTS_SHOWN = 5;
    private MostReadListCard card;
    public MostReadCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull MostReadListCard card) {
        super.setCard(card);
        header(card);
        this.card = card;
        set(new RecyclerAdapter(card.items().subList(0, Math.min(card.items().size(), EVENTS_SHOWN))));
        setMoreContentTextView(String.format(getContext().getString(R.string.all_trending_text), card.subtitle()));
    }

    private void header(@NonNull MostReadListCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_most_read)
                .setImageCircleColor(ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent))
                .setCard(card)
                .setCallback(getCallback());
        header(header);
    }

    private class RecyclerAdapter extends ListCardRecyclerAdapter<MostReadItemCard> {
        RecyclerAdapter(@NonNull List<MostReadItemCard> items) {
            super(items);
        }

        @Nullable @Override protected ListCardItemView.Callback callback() {
            return getCallback();
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder, int position) {
            MostReadItemCard item = item(position);
            holder.getView().setCard(card)
                    .setHistoryEntry(new HistoryEntry(item.pageTitle(),
                            HistoryEntry.SOURCE_FEED_MOST_READ));
        }
    }
}
