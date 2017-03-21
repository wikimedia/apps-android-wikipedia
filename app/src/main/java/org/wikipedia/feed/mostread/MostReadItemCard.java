package org.wikipedia.feed.mostread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.FeedPageSummary;
import org.wikipedia.page.PageTitle;

public class MostReadItemCard extends Card {
    @NonNull private final FeedPageSummary page;
    @NonNull private final WikiSite wiki;

    MostReadItemCard(@NonNull FeedPageSummary page, @NonNull WikiSite wiki) {
        this.page = page;
        this.wiki = wiki;
    }

    @NonNull @Override public String title() {
        return page.getNormalizedTitle();
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
        PageTitle title = new PageTitle(page.getTitle(), wiki);
        if (page.getThumbnailUrl() != null) {
            title.setThumbUrl(page.getThumbnailUrl());
        }
        if (!TextUtils.isEmpty(page.getDescription())) {
            title.setDescription(page.getDescription());
        }
        return title;
    }
}
