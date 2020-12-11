package org.wikipedia.feed.news;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ImageUrlUtil;

import static org.wikipedia.dataclient.Service.PREFERRED_THUMB_SIZE;

class NewsLinkCard extends Card {
    @NonNull private PageSummary page;
    @NonNull private WikiSite wiki;

    NewsLinkCard(@NonNull PageSummary page, @NonNull WikiSite wiki) {
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
        return thumbUrl != null ? Uri.parse(ImageUrlUtil.getUrlForPreferredSize(thumbUrl, PREFERRED_THUMB_SIZE)) : null;
    }

    @NonNull @Override public CardType type() {
        return CardType.NEWS_ITEM_LINK;
    }

    @NonNull public PageTitle pageTitle() {
        return page.getPageTitle(wiki);
    }
}
