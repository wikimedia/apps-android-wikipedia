package org.wikipedia.feed.news;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.feed.model.CardPageItem;

import java.util.List;

public class NewsItem {
    @SuppressWarnings("unused,NullableProblems") @NonNull private String story;
    @SuppressWarnings("unused,NullableProblems") @NonNull private List<CardPageItem> links;

    @NonNull
    public String story() {
        return story;
    }

    @NonNull
    public List<CardPageItem> links() {
        return links;
    }

    @Nullable
    public Uri image() {
        return getFirstImageUri(links);
    }

    /**
     * Iterate through the CardPageItems associated with the news story's links and return the first
     * thumb URI found.
     */
    @Nullable
    private Uri getFirstImageUri(List<CardPageItem> links) {
        for (CardPageItem link : links) {
            Uri thumbnail = link.thumbnail();
            if (thumbnail != null) {
                return thumbnail;
            }
        }
        return null;
    }
}
