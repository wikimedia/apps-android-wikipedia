package org.wikipedia.feed.demo;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

// todo: [Feed] remove.
public class IntegerListCardView extends ListCardView<IntegerListCard> {
    public IntegerListCardView(Context context) {
        super(context);
    }

    public void set(@NonNull IntegerListCard card) {
        header(card);
        footer(card);
        set(new RecyclerAdapter(card.items()));
    }

    private void header(@NonNull IntegerListCard card) {
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle());
        header(header);
    }

    private void footer(@NonNull IntegerListCard card) {
        CardFooterView footer = new CardFooterView(getContext())
                .setText(card.footer());
        footer.setVisibility(card.items().size() > 2 ? VISIBLE : GONE);
        footer(footer);
    }

    private static class RecyclerAdapter extends ListCardView.RecyclerAdapter<IntegerListItemCard> {
        RecyclerAdapter(@NonNull List<IntegerListItemCard> items) {
            super(items);
        }

        @Override public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder,
                                               int position) {
            IntegerListItemCard card = item(position);
            holder.getView().setTitle(card.title());
            holder.getView().setSubtitle(card.subtitle());
        }
    }
}