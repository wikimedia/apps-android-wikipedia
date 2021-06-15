package org.wikipedia.feed.topread;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.page.PageTitle;

import java.util.Collections;
import java.util.List;

public class TopReadItemCard extends Card {
    @NonNull private final TopReadArticles page;
    @NonNull private final WikiSite wiki;

    TopReadItemCard(@NonNull TopReadArticles page, @NonNull WikiSite wiki) {
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

    public int getPageViews() {
        return page.getViews();
    }

    @NonNull
    public List<TopReadArticles.ViewHistory> getViewHistory() {
        return page.getViewHistory() != null ? page.getViewHistory() : Collections.emptyList();
    }

    @NonNull @Override public CardType type() {
        return CardType.MOST_READ_ITEM;
    }

    @NonNull public PageTitle pageTitle() {
        return page.getPageTitle(wiki);
    }
}
