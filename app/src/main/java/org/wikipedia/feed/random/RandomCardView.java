package org.wikipedia.feed.random;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomArticleRequestHandler;

public class RandomCardView extends StaticCardView<RandomCard> {
    public interface Callback {
        void onGetRandomError(@NonNull Throwable t, @NonNull RandomCardView view);
    }

    public RandomCardView(@NonNull Context context) {
        super(context);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getRandomPage();
            }
        });
    }

    @Override public void setCard(@NonNull RandomCard card) {
        super.setCard(card);
        setTitle(getString(R.string.view_random_card_title));
        setSubtitle(getString(R.string.view_random_card_subtitle));
        setIcon(R.drawable.icon_feed_random);
    }

    public void getRandomPage() {
        if (getCallback() != null && getCard() != null) {
            setProgress(true);
            RandomArticleRequestHandler.getRandomPage(new RandomArticleRequestHandler.Callback() {
                @Override
                public void onSuccess(@NonNull PageTitle pageTitle) {
                    if (getCallback() != null && getCard() != null) {
                        getCallback().onSelectPage(getCard(),
                                new HistoryEntry(pageTitle, HistoryEntry.SOURCE_FEED_RANDOM));
                    }
                    setProgress(false);
                }

                @Override
                public void onError(Throwable t) {
                    getCallback().onGetRandomError(t, RandomCardView.this);
                    setProgress(false);
                }
            });
        }
    }
}
