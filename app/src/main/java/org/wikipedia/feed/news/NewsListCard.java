package org.wikipedia.feed.news;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.feed.model.UtcDate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NewsListCard extends ListCard<NewsItemCard> {
    @NonNull private UtcDate age;

    public NewsListCard(@NonNull List<NewsItem> news, @NonNull UtcDate age, @NonNull WikiSite wiki) {
        super(toItemCards(news, wiki));
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

    @NonNull @VisibleForTesting
    public static List<NewsItemCard> toItemCards(@NonNull List<NewsItem> items, @NonNull WikiSite wiki) {
        List<NewsItemCard> itemCards = new ArrayList<>();
        for (NewsItem item : items) {
            itemCards.add(new NewsItemCard(item, wiki));
        }
        return itemCards;
    }

    @Override
    protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(age.baseCalendar().getTime().getTime());
    }
}
