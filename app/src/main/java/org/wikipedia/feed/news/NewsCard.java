package org.wikipedia.feed.news;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.util.L10nUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class NewsCard extends WikiSiteCard {
    @NonNull private UtcDate date;
    @NonNull private List<NewsItem> news;

    public NewsCard(@NonNull List<NewsItem> news, int age, @NonNull WikiSite wiki) {
        super(wiki);
        this.news = news;
        this.date = new UtcDate(age);
    }

    @NonNull @Override public String title() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_card_news_title);
    }

    @NonNull @Override public CardType type() {
        return CardType.NEWS_LIST;
    }

    @NonNull public UtcDate date() {
        return date;
    }
    @NonNull public List<NewsItem> news() {
        return news;
    }

    @Override protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(date.getBaseCalendar().getTime().getTime()) + wikiSite().hashCode();
    }
}
