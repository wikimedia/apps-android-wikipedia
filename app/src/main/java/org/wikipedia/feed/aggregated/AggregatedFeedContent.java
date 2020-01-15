package org.wikipedia.feed.aggregated;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.mostread.MostReadArticles;
import org.wikipedia.feed.news.NewsItem;
import org.wikipedia.feed.onthisday.OnThisDay;

import java.util.List;

public class AggregatedFeedContent {
    @SuppressWarnings("unused") @Nullable private PageSummary tfa;
    @SuppressWarnings("unused") @Nullable private List<NewsItem> news;
    @SuppressWarnings("unused") @SerializedName("mostread") @Nullable private MostReadArticles mostRead;
    @SuppressWarnings("unused") @Nullable private FeaturedImage image;
    @SuppressWarnings("unused") @Nullable private List<OnThisDay.Event> onthisday;

    @Nullable
    public List<OnThisDay.Event> onthisday() {
        return onthisday;
    }

    @Nullable
    public PageSummary tfa() {
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
