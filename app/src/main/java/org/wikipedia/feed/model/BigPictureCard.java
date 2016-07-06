package org.wikipedia.feed.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.StringUtil;

public abstract class BigPictureCard extends Card {
    @NonNull private CardPageItem page;
    @NonNull private UtcDate age;
    @NonNull private Site site;

    public BigPictureCard(@NonNull CardPageItem page, @NonNull UtcDate age, @NonNull Site site) {
        this.page = page;
        this.age = age;
        this.site = site;
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(age.baseCalendar());
    }

    @NonNull
    public String articleTitle() {
        return page.title();
    }

    @Nullable
    public String articleSubtitle() {
        return page.description() != null
                ? StringUtil.capitalizeFirstChar(page.description()) : null;
    }

    @Override
    @Nullable
    public Uri image() {
        return page.thumbnail();
    }

    @Nullable
    @Override
    public String extract() {
        return page.extract();
    }

    @NonNull
    public HistoryEntry historyEntry(int source) {
        PageTitle title = new PageTitle(articleTitle(), site);
        if (image() != null) {
            title.setThumbUrl(image().toString());
        }
        title.setDescription(articleSubtitle());
        return new HistoryEntry(title, source);
    }
}
