package org.wikipedia.feed.featured;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.FeedPageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DateUtil;

public class FeaturedArticleCard extends Card {
    @NonNull private FeedPageSummary page;
    private int age;
    @NonNull private WikiSite wiki;

    public FeaturedArticleCard(@NonNull FeedPageSummary page, int age, @NonNull WikiSite wiki) {
        this.page = page;
        this.age = age;
        this.wiki = wiki;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_featured_article_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(age);
    }

    @NonNull
    public WikiSite wikiSite() {
        return wiki;
    }

    @NonNull
    String articleTitle() {
        return page.getNormalizedTitle();
    }

    @Nullable
    String articleSubtitle() {
        return page.getDescription() != null
                ? StringUtils.capitalize(page.getDescription()) : null;
    }

    @Override
    @Nullable
    public Uri image() {
        String thumbUrl = page.getThumbnailUrl();
        return thumbUrl != null ? Uri.parse(thumbUrl) : null;
    }

    @Nullable
    @Override
    public String extract() {
        return page.getExtract();
    }

    @NonNull @Override public CardType type() {
        return CardType.FEATURED_ARTICLE;
    }

    @NonNull
    public HistoryEntry historyEntry(int source) {
        PageTitle title = new PageTitle(articleTitle(), wikiSite());
        if (image() != null) {
            title.setThumbUrl(image().toString());
        }
        title.setDescription(articleSubtitle());
        return new HistoryEntry(title, source);
    }

    @Override
    protected int dismissHashCode() {
        return page.getTitle().hashCode();
    }
}
