package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.feed.demo.IntegerListCard;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class MostReadCardView extends ListCardView<IntegerListCard> {
    public MostReadCardView(Context context) {
        super(context);
    }

    public void set(@NonNull MostReadListCard card) {
        header(card);
        footer(card);
        set(new RecyclerAdapter(card.items()));
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

    private static class RecyclerAdapter extends ListCardView.RecyclerAdapter<MostReadItemCard> {
        RecyclerAdapter(@NonNull List<MostReadItemCard> items) {
            super(items);
        }

        @Override public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder,
                                               int position) {
            MostReadItemCard card = item(position);
            holder.getView().setTitle(card.title());
            holder.getView().setSubtitle(card.subtitle());
            holder.getView().setImage(card.image());
        }
    }
}