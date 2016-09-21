package org.wikipedia.feed.random;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomSummaryService;
import org.wikipedia.util.log.L;

public class RandomCardView extends StaticCardView<RandomCard> {
    public RandomCardView(@NonNull Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull RandomCard card) {
        super.setCard(card);
        setTitle(getString(R.string.view_random_card_title));
        setSubtitle(getString(R.string.view_random_card_subtitle));
        setIcon(R.drawable.icon_feed_random);
    }

    @Override public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        setOnClickListener(new CallbackAdapter());
    }

    private class CallbackAdapter implements OnClickListener {
        @Override
        public void onClick(View view) {
            if (getCallback() != null && getCard() != null) {
                new RandomSummaryService(getCard().site(), serviceCallback).get();
            }
        }

        private RandomSummaryService.RandomSummaryCallback serviceCallback
                = new RandomSummaryService.RandomSummaryCallback() {
            @Override
            public void onSuccess(PageTitle title) {
                if (getCallback() != null) {
                    getCallback().onSelectPage(new HistoryEntry(title,
                            HistoryEntry.SOURCE_FEED_RANDOM));
                }
            }

            @Override
            public void onError(Throwable t) {
                L.w("Failed to get random card", t);
            }
        };
    }
}