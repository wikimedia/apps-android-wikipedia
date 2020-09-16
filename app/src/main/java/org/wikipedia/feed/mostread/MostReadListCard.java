package org.wikipedia.feed.mostread;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.L10nUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MostReadListCard extends ListCard<MostReadItemCard> {
    @NonNull private final MostReadArticles articles;
    @NonNull private WikiSite wiki;

    public MostReadListCard(@NonNull MostReadArticles articles, @NonNull WikiSite wiki) {
        super(toItems(articles.articles(), wiki), wiki);
        this.articles = articles;
        this.wiki = wiki;
    }

    @NonNull @Override public String title() {
        return L10nUtil.getStringForArticleLanguage(wiki.languageCode(), R.string.view_top_read_card_title);
    }

    @Nullable @Override public String subtitle() {
        return DateUtil.getFeedCardDateString(articles.date());
    }

    @NonNull @Override public CardType type() {
        return CardType.MOST_READ_LIST;
    }

    @NonNull @VisibleForTesting
    public static List<MostReadItemCard> toItems(@NonNull List<PageSummary> articles,
                                          @NonNull WikiSite wiki) {
        List<MostReadItemCard> cards = new ArrayList<>();
        for (PageSummary article : articles) {
            cards.add(new MostReadItemCard(article, wiki));
        }
        return cards;
    }

    @NonNull private String getString(@StringRes int id, @Nullable Object... formatArgs) {
        return context().getString(id, formatArgs);
    }

    @NonNull private Context context() {
        return WikipediaApp.getInstance();
    }

    @Override
    protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(articles.date().getTime()) + wikiSite().hashCode();
    }
}
