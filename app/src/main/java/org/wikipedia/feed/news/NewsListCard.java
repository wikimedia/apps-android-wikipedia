package org.wikipedia.feed.news;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.feed.model.ListCard;

import java.util.ArrayList;
import java.util.List;

public class NewsListCard extends ListCard<NewsItemCard> {
    @NonNull private Site site;
    @NonNull private UtcDate age;

    public NewsListCard(@NonNull List<NewsItem> news, @NonNull UtcDate age, @NonNull Site site) {
        super(toItemCards(news));
        this.age = age;
        this.site = site;
    }

    @NonNull
    @Override
    public String title() {
        return "";
    }

    @NonNull
    public UtcDate age() {
        return age;
    }

    @NonNull
    private static List<NewsItemCard> toItemCards(@NonNull List<NewsItem> items) {
        List<NewsItemCard> itemCards = new ArrayList<>();
        for (NewsItem item : items) {
            itemCards.add(new NewsItemCard(item));
        }
        return itemCards;
    }
}
