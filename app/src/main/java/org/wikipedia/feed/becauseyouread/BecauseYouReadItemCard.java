package org.wikipedia.feed.becauseyouread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.feed.model.Card;
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
}
