package org.wikipedia.feed.topread;

import androidx.annotation.NonNull;

import org.wikipedia.json.annotations.Required;

import java.util.Date;
import java.util.List;

@SuppressWarnings("unused,NullableProblems")
public final class TopRead {
    @Required @NonNull private Date date;
    @Required @NonNull private List<TopReadArticles> articles;

    @NonNull public Date date() {
        return date;
    }

    @NonNull public List<TopReadArticles> articles() {
        return articles;
    }
}
