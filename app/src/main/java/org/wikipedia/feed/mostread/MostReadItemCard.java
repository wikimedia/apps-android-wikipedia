package org.wikipedia.feed.mostread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.model.Card;

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
        return article.thumbnail();
    }
}