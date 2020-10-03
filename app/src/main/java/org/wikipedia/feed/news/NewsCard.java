package org.wikipedia.feed.news;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.util.L10nUtil;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public class NewsCard extends WikiSiteCard {
    @NonNull private final LocalDate date;
    @NonNull private final List<NewsItem> news;

    public NewsCard(@NonNull List<NewsItem> news, int age, @NonNull WikiSite wiki) {
        super(wiki);
        this.news = news;
        this.date = LocalDate.now(ZoneOffset.UTC).minusDays(age);
    }

    @NonNull @Override public String title() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_card_news_title);
    }

    @NonNull @Override public CardType type() {
        return CardType.NEWS_LIST;
    }

    @NonNull public LocalDate date() {
        return date;
    }
    @NonNull public List<NewsItem> news() {
        return news;
    }

    @Override protected int dismissHashCode() {
        return (int) date.toEpochDay() + wikiSite().hashCode();
    }
}
