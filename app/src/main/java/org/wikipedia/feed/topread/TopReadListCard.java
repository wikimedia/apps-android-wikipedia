package org.wikipedia.feed.topread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.L10nUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TopReadListCard extends ListCard<TopReadItemCard> {
    @NonNull private final TopRead articles;

    public TopReadListCard(@NonNull TopRead articles, @NonNull WikiSite wiki) {
        super(toItems(articles.articles(), wiki), wiki);
        this.articles = articles;
    }

    @NonNull
    @Override public String title() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_top_read_card_title);
    }

    @Nullable
    @Override public String subtitle() {
        return DateUtil.getFeedCardDateString(articles.date());
    }

    @NonNull
    @Override public CardType type() {
        return CardType.TOP_READ_LIST;
    }

    @NonNull
    public String footerActionText() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_top_read_card_action);
    }

    @NonNull
    @VisibleForTesting
    public static List<TopReadItemCard> toItems(@NonNull List<TopReadArticles> articles,
                                                @NonNull WikiSite wiki) {
        List<TopReadItemCard> cards = new ArrayList<>();
        for (TopReadArticles article : articles) {
            cards.add(new TopReadItemCard(article, wiki));
        }
        return cards;
    }

    @Override
    protected int dismissHashCode() {
        return (int) TimeUnit.MILLISECONDS.toDays(articles.date().getTime()) + wikiSite().hashCode();
    }
}
