package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.PageTitleListCardItemView;
import org.wikipedia.feed.view.PageTitleListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class BecauseYouReadCardView extends PageTitleListCardView<BecauseYouReadCard> {

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
                .setImageCircleColor(R.color.gray_highlight);
        header(header);
        CardLargeHeaderView largeHeader = new CardLargeHeaderView(getContext())
                .setSubtitle(card.subtitle())
                .setPageTitle(card.pageTitle())
                .setImage(card.image());
        largeHeader(largeHeader);
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