package org.wikipedia.feed.random;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.featured.FeaturedArticleCard;

public class NewRandomCard extends FeaturedArticleCard {
    public NewRandomCard(@NonNull PageSummary page, int age, @NonNull WikiSite wiki) {
        super(page, age, wiki);
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_random_article_card_title);
    }
}
