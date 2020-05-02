package org.wikipedia.feed.becauseyouread;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
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

    @NonNull @Override public CardType type() {
        return CardType.BECAUSE_YOU_READ_LIST;
    }

    public String pageTitle() {
        return entry.getTitle().getDisplayText();
    }

    @NonNull public PageTitle getPageTitle() {
        return entry.getTitle();
    }

    /** @return The last visit age in days. */
    public long daysOld() {
        return Period.between(LocalDate.now(), LocalDate.from(entry.getTimestamp())).getDays();
    }

    @Override
    protected int dismissHashCode() {
        return entry.getTitle().hashCode();
    }
}
