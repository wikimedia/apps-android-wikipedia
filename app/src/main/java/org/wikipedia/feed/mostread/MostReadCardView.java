package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.demo.IntegerListCard;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.PageTitleListCardItemView;
import org.wikipedia.feed.view.PageTitleListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class MostReadCardView extends PageTitleListCardView<IntegerListCard> {
    public MostReadCardView(Context context) {
        super(context);
    }

    public void set(@NonNull MostReadListCard card) {
        header(card);
        //TODO: add footer when ready.
        //footer(card);
        set(new RecyclerAdapter(card.items(), getCallback()));
    }

    private void header(@NonNull MostReadListCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_most_read)
                .setImageCircleColor(R.color.blue_progressive);
        header(header);
    }

    private void footer(@NonNull MostReadListCard card) {
        CardFooterView footer = new CardFooterView(getContext())
                .setText(card.footer());
        footer.setVisibility(card.items().size() > 2 ? VISIBLE : GONE);
        footer(footer);
    }

    private static class RecyclerAdapter extends PageTitleListCardView.RecyclerAdapter<MostReadItemCard> {
        @Nullable private FeedViewCallback callback;

        RecyclerAdapter(@NonNull List<MostReadItemCard> items, @Nullable FeedViewCallback callback) {
            super(items);
            this.callback = callback;
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<PageTitleListCardItemView> holder, int position) {
            MostReadItemCard card = item(position);
            holder.getView().setPageTitle(card.pageTitle());
            holder.getView().setCallback(callback);
        }
    }
}