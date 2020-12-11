package org.wikipedia.feed.mostread;

import androidx.annotation.NonNull;

import org.wikipedia.json.annotations.Required;

import java.util.Date;
import java.util.List;

@SuppressWarnings("unused,NullableProblems")
public final class MostRead {
    @Required @NonNull private Date date;
    @Required @NonNull private List<MostReadArticles> articles;

    @NonNull public Date date() {
        return date;
    }

    @NonNull public List<MostReadArticles> articles() {
        return articles;
    }
}
