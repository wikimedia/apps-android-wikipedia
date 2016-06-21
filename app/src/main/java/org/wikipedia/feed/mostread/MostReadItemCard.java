package org.wikipedia.feed.mostread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.log.L;

public class MostReadItemCard extends Card {
    @NonNull private final MostReadArticle article;

    public MostReadItemCard(@NonNull MostReadArticle article) {
        this.article = article;
    }

    @NonNull @Override public String title() {
        return article.normalizedTitle();
    }

    @Nullable @Override public String subtitle() {
        return article.description();
    }

    @Nullable @Override public Uri image() {
        L.d(article.thumbnails().toString());
        return article.thumbnails().get(Constants.PREFERRED_THUMB_SIZE);
    }
}