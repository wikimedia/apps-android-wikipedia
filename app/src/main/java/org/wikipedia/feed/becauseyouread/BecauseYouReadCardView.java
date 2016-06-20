package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.PageTitleListCardItemView;
import org.wikipedia.feed.view.PageTitleListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class BecauseYouReadCardView extends PageTitleListCardView<BecauseYouReadCard> {
    @Nullable private FeedViewCallback callback;

    public BecauseYouReadCardView(Context context) {
        super(context);
    }

    @NonNull public PageTitleListCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    public void set(@NonNull final BecauseYouReadCard card) {
        header(card);
        set(new RecyclerAdapter(card.items(), callback));
    }

    private void header(@NonNull final BecauseYouReadCard card) {
        CardLargeHeaderView header = new CardLargeHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setPageTitle(card.pageTitle())
                .setImage(card.image());
        header(header);
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
            holder.getView().setPageTitle(card.pageTitle());
            holder.getView().setCallback(callback);
        }
    }
}