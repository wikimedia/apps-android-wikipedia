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

    @Override
    @NonNull
    public String footerActionText() {
        return WikipediaApp.getInstance().getString(R.string.view_random_article_card_action);
    }

    @NonNull
    @Override public CardType type() {
        return CardType.RANDOM;
    }

    public int historyEntrySource() {
        return HistoryEntry.SOURCE_FEED_RANDOM;
    }
}
