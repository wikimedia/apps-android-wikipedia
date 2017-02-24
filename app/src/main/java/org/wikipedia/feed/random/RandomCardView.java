package org.wikipedia.feed.random;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomSummaryClient;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.util.log.L;

import retrofit2.Call;

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
                setProgress(true);
                new RandomSummaryClient().request(getCard().wikiSite(), serviceCallback);
            }
        }

        private RandomSummaryClient.Callback serviceCallback = new RandomSummaryClient.Callback() {
            @Override
            public void onSuccess(@NonNull Call<RbPageSummary> call, @NonNull PageTitle title) {
                setProgress(false);
                if (getCallback() != null) {
                    getCallback().onSelectPage(new HistoryEntry(title,
                            HistoryEntry.SOURCE_FEED_RANDOM));
                }
            }

            @Override
            public void onError(@NonNull Call<RbPageSummary> call, @NonNull Throwable t) {
                L.w("Failed to get random card from network. Falling back to reading lists.", t);
                getRandomReadingListPage();
                setProgress(false);
            }
        };

        private void getRandomReadingListPage() {
            ReadingListPageDao.instance().randomPage(new CallbackTask.Callback<PageTitle>() {
                @Override public void success(@Nullable PageTitle title) {
                    if (getCallback() != null) {
                        if (title != null) {
                            getCallback().onSelectPage(new HistoryEntry(title,
                                    HistoryEntry.SOURCE_FEED_RANDOM));
                        } else {
                            getCallback().onError(new RuntimeException(getString(R.string.view_random_card_error)));
                        }
                    }
                }
            });
        }
    }
}
