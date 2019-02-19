package org.wikipedia.feed.featured;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DateUtils;

public class FeaturedArticleCard extends WikiSiteCard {
    @NonNull private RbPageSummary page;
    private int age;

    public FeaturedArticleCard(@NonNull RbPageSummary page, int age, @NonNull WikiSite wiki) {
        super(wiki);
        this.page = page;
        this.age = age;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_featured_article_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtils.getFeedCardDateString(age);
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
        return page.getExtractHtml();
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
