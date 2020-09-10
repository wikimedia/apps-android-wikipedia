package org.wikipedia.feed.mostread;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardRecyclerAdapter;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class MostReadCardView extends ListCardView<MostReadListCard> {
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
        setMoreContentTextView(getContext().getString(R.string.more_trending_text));
        setLayoutDirectionByWikiSite(card.wikiSite(), getLayoutDirectionView());
    }

    private void header(@NonNull MostReadListCard card) {
        headerView().setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_most_read)
                .setImageCircleColor(ResourceUtil.getThemedAttributeId(getContext(), R.attr.colorAccent))
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());
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
            holder.getView().setCard(card)
                    .setHistoryEntry(new HistoryEntry(item.pageTitle(),
                            HistoryEntry.SOURCE_FEED_MOST_READ));
        }
    }
}
