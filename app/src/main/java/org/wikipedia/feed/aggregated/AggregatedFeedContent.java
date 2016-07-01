package org.wikipedia.feed.aggregated;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.feed.mostread.MostReadArticles;
import org.wikipedia.feed.news.NewsItem;

import java.util.List;

public class AggregatedFeedContent {
    @SuppressWarnings("unused") @Nullable private CardPageItem tfa;
    @SuppressWarnings("unused") @Nullable private List<NewsItem> news;
    @SuppressWarnings("unused") @Nullable private CardPageItem random;
    @SuppressWarnings("unused") @SerializedName("mostread") @Nullable private MostReadArticles mostRead;

    @Nullable
    public CardPageItem tfa() {
        return tfa;
    }

    @Nullable
    public List<NewsItem> news() {
        return news;
    }

    @Nullable
    public MostReadArticles mostRead() {
        return mostRead;
    }
}