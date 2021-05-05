package org.wikipedia.feed.mostread;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardRecyclerAdapter;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class MostReadCardView extends ListCardView<MostReadListCard> {
    private static final int EVENTS_SHOWN = 5;
    public MostReadCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull MostReadListCard card) {
        super.setCard(card);
        header();
        footer();
        set(new RecyclerAdapter(card.items().subList(0, Math.min(card.items().size(), EVENTS_SHOWN))));
        setLayoutDirectionByWikiSite(card.wikiSite(), getLayoutDirectionView());
    }

    private void footer() {
        if (getCard() == null) {
            return;
        }
        getFooterView().setVisibility(View.VISIBLE);
        getFooterView().setCallback(getFooterCallback(getCard()));
        getFooterView().setFooterActionText(getCard().footerActionText(), getCard().wikiSite().languageCode());
    }

    private void header() {
        if (getCard() == null) {
            return;
        }
        getHeaderView().setTitle(getCard().title())
                .setLangCode(getCard().wikiSite().languageCode())
                .setCard(getCard())
                .setCallback(getCallback());
    }

    public CardFooterView.Callback getFooterCallback(@NonNull MostReadListCard card) {
        return () -> {
            if (getCallback() != null) {
                getCallback().onFooterClick(card);
            }
        };
    }

    private class RecyclerAdapter extends ListCardRecyclerAdapter<MostReadItemCard> {
        RecyclerAdapter(@NonNull List<MostReadItemCard> items) {
            super(items);
        }

        @Nullable @Override protected ListCardItemView.Callback callback() {
            return getCallback();
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<ListCardItemView> holder, int position) {
            MostReadItemCard item = item(position);
            holder.getView().setCard(getCard())
                    .setHistoryEntry(new HistoryEntry(item.pageTitle(),
                            HistoryEntry.SOURCE_FEED_MOST_READ));
            holder.getView().setNumber(position + 1);
            holder.getView().setPageViews(item.getPageViews());
            holder.getView().setGraphView(item.getViewHistory());
        }
    }
}
