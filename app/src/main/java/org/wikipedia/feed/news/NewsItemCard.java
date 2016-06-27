package org.wikipedia.feed.news;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardPageItem;

import java.util.List;

public class NewsItemCard extends Card {
    @NonNull private NewsItem newsItem;

    public NewsItemCard(@NonNull NewsItem item) {
        this.newsItem = item;
    }

    @Nullable
    @Override
    public Uri image() {
        return newsItem.image();
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
