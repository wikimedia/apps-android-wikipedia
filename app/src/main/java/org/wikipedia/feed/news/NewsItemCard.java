package org.wikipedia.feed.news;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardPageItem;

import java.util.List;

public class NewsItemCard extends Card {
    @NonNull private NewsItem newsItem;
    @NonNull private Site site;

    public NewsItemCard(@NonNull NewsItem item, @NonNull Site site) {
        this.newsItem = item;
        this.site = site;
    }

    @NonNull
    public NewsItem item() {
        return newsItem;
    }

    @NonNull
    public Site site() {
        return site;
    }

    @Nullable
    @Override
    public Uri image() {
        return newsItem.thumb();
    }

    @NonNull
    public Spanned text() {
        return Html.fromHtml(newsItem.story());
    }

    public List<CardPageItem> links() {
        return newsItem.links();
    }

    // Unused
    @NonNull
    @Override
    public String title() {
        return "";
    }
}
