package org.wikipedia.feed.aggregated;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.model.FeedPageSummary;
import org.wikipedia.feed.mostread.MostReadArticles;
import org.wikipedia.feed.news.NewsItem;

import java.util.List;

class AggregatedFeedContent {
    @SuppressWarnings("unused") @Nullable private FeedPageSummary tfa;
    @SuppressWarnings("unused") @Nullable private List<NewsItem> news;
    @SuppressWarnings("unused") @SerializedName("mostread") @Nullable private MostReadArticles mostRead;
    @SuppressWarnings("unused") @Nullable private FeaturedImage image;

    @Nullable
    FeedPageSummary tfa() {
        return tfa;
    }

    @Nullable
    List<NewsItem> news() {
        return news;
    }

    @Nullable
    MostReadArticles mostRead() {
        return mostRead;
    }

    @Nullable
    FeaturedImage potd() {
        return image;
    }
}
