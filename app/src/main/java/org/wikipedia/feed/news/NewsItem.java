package org.wikipedia.feed.news;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.json.annotations.Required;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.wikipedia.dataclient.Service.PREFERRED_THUMB_SIZE;
import static org.wikipedia.util.ImageUrlUtil.getUrlForSize;

public final class NewsItem {
    @SuppressWarnings("unused") @Required @Nullable private String story;
    @SuppressWarnings("unused") @Nullable private List<PageSummary> links
            = Collections.emptyList();

    @NonNull String story() {
        return StringUtils.defaultString(story);
    }

    @NonNull public List<PageSummary> links() {
        return links != null ? links : Collections.emptyList();
    }

    @NonNull List<NewsLinkCard> linkCards(WikiSite wiki) {
        List<NewsLinkCard> linkCards = new ArrayList<>();
        for (PageSummary link : links()) {
            if (link == null) {
                continue;
            }
            linkCards.add(new NewsLinkCard(link, wiki));
        }
        return linkCards;
    }

    @Nullable public Uri thumb() {
        Uri uri = getFirstImageUri(links());
        return uri != null ? getUrlForSize(uri, PREFERRED_THUMB_SIZE) : null;
    }

    @Nullable Uri featureImage() {
        return getFirstImageUri(links());
    }

    /**
     * Iterate through the CardPageItems associated with the news story's links and return the first
     * thumb URI found.
     */
    @Nullable private Uri getFirstImageUri(List<PageSummary> links) {
        for (PageSummary link : links) {
            if (link == null) {
                continue;
            }
            String thumbnail = link.getThumbnailUrl();
            if (thumbnail != null) {
                return Uri.parse(thumbnail);
            }
        }
        return null;
    }
}
