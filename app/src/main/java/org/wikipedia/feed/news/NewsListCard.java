package org.wikipedia.feed.news;

import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.feed.model.ListCard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @NonNull @Override public CardType type() {
        return CardType.NEWS_LIST;
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

    @Override
    protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(age.baseCalendar().getTime().getTime());
    }
}
