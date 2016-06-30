package org.wikipedia.feed.mostread;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.List;

public final class MostReadArticles {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Date date;
    @SuppressWarnings("unused,NullableProblems") @NonNull private List<MostReadArticle> articles;

    @NonNull public Date date() {
        return date;
    }

    @NonNull public List<MostReadArticle> articles() {
        return articles;
    }
}