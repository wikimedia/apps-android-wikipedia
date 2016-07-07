package org.wikipedia.feed.becauseyouread;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.page.PageTitle;

import java.util.List;

public class BecauseYouReadCard extends ListCard<BecauseYouReadItemCard> {
    @NonNull private PageTitle title;

    public BecauseYouReadCard(@NonNull final PageTitle title,
                              @NonNull final List<BecauseYouReadItemCard> itemCards) {
        super(itemCards);
        this.title = title;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_because_you_read_card_title);
    }

    @Override
    @Nullable
    public Uri image() {
        return TextUtils.isEmpty(title.getThumbUrl()) ? null : Uri.parse(title.getThumbUrl());
    }

    public String pageTitle() {
        return title.getDisplayText();
    }

    @NonNull public PageTitle getPageTitle() {
        return title;
    }
}