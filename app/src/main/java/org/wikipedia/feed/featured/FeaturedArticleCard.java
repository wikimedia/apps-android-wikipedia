package org.wikipedia.feed.featured;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.feed.model.BigPictureCard;
import org.wikipedia.feed.model.CardPageItem;

public class FeaturedArticleCard extends BigPictureCard {

    public FeaturedArticleCard(@NonNull CardPageItem page, @NonNull UtcDate date, @NonNull Site site) {
        super(page, date, site);
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_featured_article_card_title);
    }
}
