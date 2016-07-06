package org.wikipedia.feed.featured;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.StringUtil;

public class FeaturedArticleCard extends Card {
    @NonNull private UtcDate date;
    @NonNull private Site site;
    @NonNull private CardPageItem page;

    public FeaturedArticleCard(@NonNull CardPageItem page, @NonNull UtcDate date, @NonNull Site site) {
        this.page = page;
        this.site = site;
        this.date = date;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_featured_article_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(date.baseCalendar());
    }

    @NonNull
    public Site site() {
        return site;
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
        PageTitle title = new PageTitle(articleTitle(), site());
        if (image() != null) {
            title.setThumbUrl(image().toString());
        }
        title.setDescription(articleSubtitle());
        return new HistoryEntry(title, source);
    }
}
