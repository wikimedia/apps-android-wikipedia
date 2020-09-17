package org.wikipedia.feed.featured;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.L10nUtil;

public class FeaturedArticleCard extends WikiSiteCard {
    @NonNull private PageSummary page;
    private int age;

    public FeaturedArticleCard(@NonNull PageSummary page, int age, @NonNull WikiSite wiki) {
        super(wiki);
        this.page = page;
        this.age = age;
    }

    @Override
    @NonNull
    public String title() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_featured_article_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(age);
    }

    @NonNull
    String articleTitle() {
        return page.getDisplayTitle();
    }

    @Nullable
    String articleSubtitle() {
        return page.getDescription();
    }

    @NonNull
    public String footerActionText() {
        return WikipediaApp.getInstance().getString(R.string.view_main_page_card_title);
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

    @NonNull
    @Override
    public CardType type() {
        return CardType.FEATURED_ARTICLE;
    }

    @NonNull
    public HistoryEntry historyEntry() {
        return new HistoryEntry(page.getPageTitle(wikiSite()), historyEntrySource());
    }

    public int historyEntrySource() {
        return HistoryEntry.SOURCE_FEED_FEATURED;
    }

    @Override
    protected int dismissHashCode() {
        return page.getApiTitle().hashCode();
    }
}
