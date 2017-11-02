package org.wikipedia.feed.mostread;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.json.annotations.Required;

import java.util.Date;
import java.util.List;

public final class MostReadArticles {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private Date date;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private List<RbPageSummary> articles;

    @NonNull public Date date() {
        return date;
    }

    @NonNull public List<RbPageSummary> articles() {
        return articles;
    }
}
