package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public class MostReadListCard extends ListCard<MostReadItemCard> {
    private static final int MAX_SIZE = 5;

    @NonNull private final MostReadArticles articles;

    public MostReadListCard(@NonNull MostReadArticles articles, @NonNull Site site) {
        super(toItems(articles.articles(), site));
        this.articles = articles;
    }

    @NonNull @Override public String title() {
        return getString(R.string.most_read_list_card_title);
    }

    @Nullable @Override public String subtitle() {
        return DateUtil.getFeedCardDateString(articles.date());
    }

    @NonNull private static List<MostReadItemCard> toItems(@NonNull List<MostReadArticle> articles,
                                                           @NonNull Site site) {
        List<MostReadItemCard> cards = new ArrayList<>();
        for (MostReadArticle article : articles) {
            cards.add(new MostReadItemCard(article, site));
        }
        return cards.subList(0, Math.min(cards.size(), MAX_SIZE));
    }

    @NonNull private String getString(@StringRes int id, @Nullable Object... formatArgs) {
        return context().getString(id, formatArgs);
    }

    @NonNull private Context context() {
        return WikipediaApp.getInstance();
    }
}
