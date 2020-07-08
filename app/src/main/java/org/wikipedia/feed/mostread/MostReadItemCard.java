package org.wikipedia.feed.mostread;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.page.PageTitle;

public class MostReadItemCard extends Card {
    @NonNull private final PageSummary page;
    @NonNull private final WikiSite wiki;

    MostReadItemCard(@NonNull PageSummary page, @NonNull WikiSite wiki) {
        this.page = page;
        this.wiki = wiki;
    }

    @NonNull @Override public String title() {
        return page.getDisplayTitle();
    }

    @Nullable @Override public String subtitle() {
        return page.getDescription();
    }

    @Nullable @Override public Uri image() {
        String thumbUrl = page.getThumbnailUrl();
        return thumbUrl != null ? Uri.parse(thumbUrl) : null;
    }

    @NonNull @Override public CardType type() {
        return CardType.MOST_READ_ITEM;
    }

    @NonNull public PageTitle pageTitle() {
        return page.getPageTitle(wiki);
    }
}
