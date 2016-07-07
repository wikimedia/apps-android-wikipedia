package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.PageTitleListCardItemView;
import org.wikipedia.feed.view.PageTitleListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import java.util.List;

public class BecauseYouReadCardView extends PageTitleListCardView
        implements ItemTouchHelperSwipeAdapter.SwipeableView {

    public BecauseYouReadCardView(Context context) {
        super(context);
    }

    public void set(@NonNull final BecauseYouReadCard card) {
        header(card);
        set(new RecyclerAdapter(card.items(), getCallback()));
    }

    private void header(@NonNull final BecauseYouReadCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setImage(R.drawable.ic_restore_black_24dp)
                .setImageCircleColor(R.color.gray_highlight)
                .setCard(card)
                .setCallback(getCallback());
        header(header);
        CardLargeHeaderView largeHeader = new CardLargeHeaderView(getContext())
                .setPageTitle(card.pageTitle())
                .setSubtitle(card.subtitle())
                .setImage(card.image());
        largeHeader.setOnClickListener(new SelectPageCallbackAdapter(card));
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

    private static class RecyclerAdapter extends PageTitleListCardView.RecyclerAdapter<BecauseYouReadItemCard> {
        @Nullable private FeedViewCallback callback;

        RecyclerAdapter(@NonNull List<BecauseYouReadItemCard> items, @Nullable FeedViewCallback callback) {
            super(items);
            this.callback = callback;
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<PageTitleListCardItemView> holder, int i) {
            BecauseYouReadItemCard card = item(i);
            holder.getView().setHistoryEntry(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ));
            holder.getView().setCallback(callback);
        }
    }
}
