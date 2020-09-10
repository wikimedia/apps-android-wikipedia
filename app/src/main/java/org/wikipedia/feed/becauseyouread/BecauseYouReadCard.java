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
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;

import java.util.List;

public class BecauseYouReadCard extends ListCard<BecauseYouReadItemCard> {
    @NonNull private HistoryEntry entry;

    public BecauseYouReadCard(@NonNull final HistoryEntry entry,
                              @NonNull final List<BecauseYouReadItemCard> itemCards) {
        super(itemCards, entry.getTitle().getWikiSite());
        this.entry = entry;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_because_you_read_card_title);
    }

    @Override
    @Nullable
    public Uri image() {
        return TextUtils.isEmpty(entry.getTitle().getThumbUrl()) ? null : Uri.parse(entry.getTitle().getThumbUrl());
    }

    @Override
    @NonNull
    public String extract() {
        // TODO: having correct extract
        return StringUtils.defaultString(entry.getTitle().getDescription());
    }

    @NonNull @Override public CardType type() {
        return CardType.BECAUSE_YOU_READ_LIST;
    }

    public String pageTitle() {
        return entry.getTitle().getDisplayText();
    }

    @NonNull public PageTitle getPageTitle() {
        return entry.getTitle();
    }

    @Override
    protected int dismissHashCode() {
        return entry.getTitle().hashCode();
    }
}
