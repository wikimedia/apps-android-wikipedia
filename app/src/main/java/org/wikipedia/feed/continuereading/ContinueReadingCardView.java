package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.PluralsRes;

import org.wikipedia.R;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.views.DefaultViewHolder;

import java.util.Collections;

public class ContinueReadingCardView extends ListCardView<ContinueReadingCard> {
    public ContinueReadingCardView(Context context) {
        super(context);
    }

    public void set(@NonNull ContinueReadingCard card) {
        header(card);
        set(new RecyclerAdapter(card));
    }

    private void header(@NonNull ContinueReadingCard card) {
        @PluralsRes int subtitle = R.plurals.view_continue_reading_card_subtitle;
        int age = (int) card.daysOld();
        CardHeaderView header = new CardHeaderView(getContext())
                .setTitle(R.string.view_continue_reading_card_title)
                .setSubtitle(getResources().getQuantityString(subtitle, age, age));
        header(header);
    }

    private static class RecyclerAdapter extends ListCardView.RecyclerAdapter<ContinueReadingCard> {
        RecyclerAdapter(@NonNull ContinueReadingCard card) {
            super(Collections.singletonList(card));
        }

        @Override public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder,
                                               int position) {
            ContinueReadingCard card = item(position);
            holder.getView().setTitle(card.title());
            holder.getView().setSubtitle(card.subtitle());
            holder.getView().setImage(card.image());
        }
    }
}