package org.wikipedia.feed.mostread;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.json.annotations.Required;

import java.util.Date;
import java.util.List;

public final class MostReadArticles {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private Date date;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private List<PageSummary> articles;

    @NonNull public Date date() {
        return date;
    }

    @NonNull public List<PageSummary> articles() {
        return articles;
    }
}
