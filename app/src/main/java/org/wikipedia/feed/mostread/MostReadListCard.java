package org.wikipedia.feed.mostread;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.ListCard;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MostReadListCard extends ListCard<MostReadItemCard> {
    private static final int MAX_SIZE = 5;

    @NonNull private final MostReadArticles articles;

    public MostReadListCard(@NonNull MostReadArticles articles) {
        super(toItems(articles.articles()));
        this.articles = articles;
    }

    @NonNull @Override public String title() {
        return getString(R.string.most_read_list_card_title, date());
    }

    @Nullable @Override public String subtitle() {
        return date();
    }

    @Nullable @Override public String footer() {
        // todo: the mocks show a more terse date used here but this will probably require TWN
        //       support. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        return getString(R.string.most_read_list_card_footer, date());
    }

    @VisibleForTesting @NonNull Date getDate() {
        return articles.date();
    }

    @NonNull private static List<MostReadItemCard> toItems(@NonNull List<MostReadArticle> articles) {
        List<MostReadItemCard> cards = new ArrayList<>();
        for (MostReadArticle article : articles) {
            cards.add(new MostReadItemCard(article));
        }
        return cards.subList(0, Math.min(cards.size(), MAX_SIZE));
    }

    @NonNull private String date() {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them.
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context());
        return dateFormat.format(articles.date());
    }

    @NonNull private String getString(@StringRes int id, @Nullable Object... formatArgs) {
        return context().getString(id, formatArgs);
    }

    @NonNull private Context context() {
        return WikipediaApp.getInstance();
    }
}