package org.wikipedia.feed.news;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.feed.model.ListCard;

import java.util.ArrayList;
import java.util.List;

public class NewsListCard extends ListCard<NewsItemCard> {
    @NonNull private UtcDate age;

    public NewsListCard(@NonNull List<NewsItem> news, @NonNull UtcDate age, @NonNull Site site) {
        super(toItemCards(news, site));
        this.age = age;
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
    private static List<NewsItemCard> toItemCards(@NonNull List<NewsItem> items, @NonNull Site site) {
        List<NewsItemCard> itemCards = new ArrayList<>();
        for (NewsItem item : items) {
            itemCards.add(new NewsItemCard(item, site));
        }
        return itemCards;
    }
}
