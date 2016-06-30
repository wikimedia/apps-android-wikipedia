package org.wikipedia.feed.aggregated;

import android.support.annotation.NonNull;

import org.wikipedia.feed.model.CardPageItem;

public class AggregatedFeedContent {
    @SuppressWarnings("NullableProblems") @NonNull private CardPageItem tfa;
    @SuppressWarnings("NullableProblems") @NonNull private CardPageItem random;

    public CardPageItem tfa() {
        return tfa;
    }
}
