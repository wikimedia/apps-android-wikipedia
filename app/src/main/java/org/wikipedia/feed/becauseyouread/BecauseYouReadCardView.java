package org.wikipedia.feed.becauseyouread;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.feed.view.ListCardRecyclerAdapter;
import org.wikipedia.feed.view.ListCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public class BecauseYouReadCardView extends ListCardView<BecauseYouReadCard> {

    public BecauseYouReadCardView(Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull BecauseYouReadCard card) {
        super.setCard(card);
        header(card);
        set(new RecyclerAdapter(card.items()));
        setLayoutDirectionByWikiSite(card.wikiSite(), getLayoutDirectionView());
    }

    private void header(@NonNull final BecauseYouReadCard card) {
        getHeaderView().setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());

        getLargeHeaderView().setTitle(card.pageTitle())
                .setLanguageCode(card.wikiSite().languageCode())
                .setImage(card.image())
                .setSubtitle(card.extract());

        getLargeHeaderContainer().setVisibility(VISIBLE);
        getLargeHeaderContainer().setOnClickListener(view -> {
            if (getCallback() != null) {
                getCallback().onSelectPage(card, new HistoryEntry(card.getPageTitle(),
                        HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ), getLargeHeaderView().getSharedElements());
            }
        });
    }

    private class RecyclerAdapter extends ListCardRecyclerAdapter<BecauseYouReadItemCard> {
        RecyclerAdapter(@NonNull List<BecauseYouReadItemCard> items) {
            super(items);
        }

        @Nullable @Override protected ListCardItemView.Callback callback() {
            return getCallback();
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<ListCardItemView> holder, int i) {
            BecauseYouReadItemCard card = item(i);
            holder.getView().setCard(card)
                    .setHistoryEntry(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_FEED_BECAUSE_YOU_READ));
        }
    }
}
