package org.wikipedia.news;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.restbase.RbPageSummary;

import static org.wikipedia.util.ImageUrlUtil.getUrlForSize;

public class NewsLinkCard extends Card {
    @NonNull private RbPageSummary page;
    @NonNull private WikiSite wiki;

    public NewsLinkCard(@NonNull RbPageSummary page, @NonNull WikiSite wiki) {
        this.page = page;
        this.wiki = wiki;
    }

    @NonNull @Override public String title() {
        return page.getTitle();
    }

    @Nullable @Override public String subtitle() {
        return page.getDescription();
    }

    @Nullable @Override public Uri image() {
        String thumbUrl = page.getThumbnailUrl();
        return thumbUrl != null ? getUrlForSize(Uri.parse(thumbUrl), Constants.PREFERRED_THUMB_SIZE) : null;
    }

    @NonNull @Override public CardType type() {
        return CardType.NEWS_ITEM_LINK;
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
