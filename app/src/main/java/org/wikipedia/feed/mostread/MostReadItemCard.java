package org.wikipedia.feed.mostread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.page.PageTitle;

public class MostReadItemCard extends Card {
    @NonNull private final MostReadArticle article;
    @NonNull private final WikiSite wiki;

    public MostReadItemCard(@NonNull MostReadArticle article, @NonNull WikiSite wiki) {
        this.article = article;
        this.wiki = wiki;
    }

    @NonNull @Override public String title() {
        return article.normalizedTitle();
    }

    @Nullable @Override public String subtitle() {
        return article.description();
    }

    @Nullable @Override public Uri image() {
        return article.thumbnail();
    }

    @NonNull @Override public CardType type() {
        return CardType.MOST_READ_ITEM;
    }

    @NonNull public PageTitle pageTitle() {
        PageTitle title = new PageTitle(article.title(), wiki);
        if (article.thumbnail() != null) {
            title.setThumbUrl(article.thumbnail().toString());
        }
        if (!TextUtils.isEmpty(article.description())) {
            title.setDescription(article.description());
        }
        return title;
    }
}