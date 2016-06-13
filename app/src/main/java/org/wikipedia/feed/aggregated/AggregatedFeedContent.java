package org.wikipedia.feed.aggregated;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.feed.mostread.MostReadArticles;

public class AggregatedFeedContent {
    @SuppressWarnings("NullableProblems") @NonNull private CardPageItem tfa;
    @SuppressWarnings("NullableProblems") @NonNull private MostReadArticles mostread;
    @SuppressWarnings("NullableProblems") @NonNull private CardPageItem random;
    // Note: the below just have placeholder strings for now
    @SuppressWarnings("NullableProblems") @NonNull private String news;
    @SuppressWarnings("NullableProblems") @NonNull private String image;
    @SuppressWarnings("NullableProblems") @NonNull private String video;

    public CardPageItem tfa() {
        return tfa;
    }

    public MostReadArticles mostRead() {
        return mostread;
    }
}
