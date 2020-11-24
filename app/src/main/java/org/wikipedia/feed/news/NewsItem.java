package org.wikipedia.feed.news;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.util.ImageUrlUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NewsItem {
    @SuppressWarnings("unused") @Nullable private String story;
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
        return uri != null ? Uri.parse(ImageUrlUtil.getUrlForPreferredSize(uri.toString(), Constants.PREFERRED_CARD_THUMBNAIL_SIZE)) : null;
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
