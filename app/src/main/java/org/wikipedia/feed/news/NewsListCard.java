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
    @NonNull private UtcDate date;

    public NewsListCard(@NonNull List<NewsItem> news, int age, @NonNull WikiSite wiki) {
        super(toItemCards(news, wiki));
        this.date = new UtcDate(age);
    }

    @NonNull @Override public CardType type() {
        return CardType.NEWS_LIST;
    }

    @NonNull public UtcDate date() {
        return date;
    }

    @NonNull @VisibleForTesting static List<NewsItemCard> toItemCards(@NonNull List<NewsItem> items, @NonNull WikiSite wiki) {
        List<NewsItemCard> itemCards = new ArrayList<>();
        for (NewsItem item : items) {
            itemCards.add(new NewsItemCard(item, wiki));
        }
        return itemCards;
    }

    @Override protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(date.baseCalendar().getTime().getTime());
    }
}
