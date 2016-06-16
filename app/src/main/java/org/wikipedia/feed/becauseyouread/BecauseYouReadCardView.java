package org.wikipedia.feed.becauseyouread;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.wikipedia.feed.view.CardLargeHeaderView;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class BecauseYouReadCardView extends ListCardView<BecauseYouReadCard> {
    public BecauseYouReadCardView(Context context) {
        super(context);
    }

    public void set(@NonNull final BecauseYouReadCard card) {
        header(card);
        set(new RecyclerAdapter(card.items()));
    }

    private void header(@NonNull final BecauseYouReadCard card) {
        CardLargeHeaderView header = new CardLargeHeaderView(getContext())
                .setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setPageTitle(card.pageTitle())
                .setImage(card.image());
        header(header);
    }

    private static class RecyclerAdapter extends ListCardView.RecyclerAdapter<BecauseYouReadItemCard> {
        RecyclerAdapter(@NonNull List<BecauseYouReadItemCard> items) {
            super(items);
        }

        @Override public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder, int i) {
            BecauseYouReadItemCard card = item(i);
            holder.getView().setTitle(card.title());
            holder.getView().setSubtitle(card.subtitle());

            Uri imageUri = card.image();
            if (imageUri != null) {
                holder.getView().setImage(imageUri);
            }
        }
    }
}
