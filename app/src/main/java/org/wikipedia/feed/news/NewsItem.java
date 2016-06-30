package org.wikipedia.feed.news;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.news.NewsLinkCard;

import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.ImageUrlUtil.getUrlForSize;

public final class NewsItem {
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

    @NonNull
    public List<NewsLinkCard> linkCards(Site site) {
        List<NewsLinkCard> linkCards = new ArrayList<>();
        for (CardPageItem link : links) {
            linkCards.add(new NewsLinkCard(link, site));
        }
        return linkCards;
    }

    @Nullable
    public Uri thumb() {
        Uri uri = getFirstImageUri(links);
        return uri != null ? getUrlForSize(uri, Constants.PREFERRED_THUMB_SIZE) : null;
    }

    @Nullable
    public Uri featureImage() {
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
