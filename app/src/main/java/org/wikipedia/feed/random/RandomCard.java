package org.wikipedia.feed.random;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.featured.FeaturedArticleCard;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.history.HistoryEntry;

public class RandomCard extends FeaturedArticleCard {

    public RandomCard(@NonNull PageSummary page, int age, @NonNull WikiSite wiki) {
        super(page, age, wiki);
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_random_article_card_title);
    }

    @NonNull @Override public CardType type() {
        return CardType.RANDOM;
    }

    @Override
    @NonNull
    public HistoryEntry historyEntry() {
        return new HistoryEntry(page.getPageTitle(wikiSite()), HistoryEntry.SOURCE_FEED_FEATURED);
    }
}
