package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.ListCard;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context());
        return dateFormat.format(articles.date());
    }

    @Nullable @Override public String footer() {
        return getString(R.string.most_read_list_card_footer);
    }

    @VisibleForTesting @NonNull Date date() {
        return articles.date();
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