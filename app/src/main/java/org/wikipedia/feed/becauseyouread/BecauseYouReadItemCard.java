package org.wikipedia.feed.becauseyouread;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.page.PageTitle;

public class BecauseYouReadItemCard extends Card {
    @NonNull private final PageTitle title;

    public BecauseYouReadItemCard(@NonNull PageTitle title) {
        this.title = title;
    }

    @NonNull
    public PageTitle pageTitle() {
        return title;
    }

    @NonNull
    @Override public String title() {
        return title.getDisplayText();
    }

    @Nullable
    @Override public String subtitle() {
        return title.getDescription();
    }

    @Nullable
    @Override public Uri image() {
        return TextUtils.isEmpty(title.getThumbUrl()) ? null : Uri.parse(title.getThumbUrl());
    }

    @NonNull @Override public CardType type() {
        return CardType.BECAUSE_YOU_READ_ITEM;
    }
}
