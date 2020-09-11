package org.wikipedia.feed.becauseyouread;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.page.PageTitle;

import java.util.List;

public class BecauseYouReadCard extends ListCard<BecauseYouReadItemCard> {
    @NonNull private PageTitle pageTitle;

    public BecauseYouReadCard(@NonNull final PageTitle pageTitle,
                              @NonNull final List<BecauseYouReadItemCard> itemCards) {
        super(itemCards, pageTitle.getWikiSite());
        this.pageTitle = pageTitle;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_because_you_read_card_title);
    }

    @Override
    @Nullable
    public Uri image() {
        return TextUtils.isEmpty(pageTitle.getThumbUrl()) ? null : Uri.parse(pageTitle.getThumbUrl());
    }

    @Override
    @NonNull
    public String extract() {
        return StringUtils.defaultString(pageTitle.getDescription());
    }

    @NonNull @Override public CardType type() {
        return CardType.BECAUSE_YOU_READ_LIST;
    }

    public String pageTitle() {
        return pageTitle.getDisplayText();
    }

    @NonNull public PageTitle getPageTitle() {
        return pageTitle;
    }

    @Override
    protected int dismissHashCode() {
        return pageTitle.hashCode();
    }
}
