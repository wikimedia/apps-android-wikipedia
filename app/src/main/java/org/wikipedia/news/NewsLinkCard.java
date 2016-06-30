package org.wikipedia.news;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.Constants;
import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.CardPageItem;
import org.wikipedia.page.PageTitle;

import static org.wikipedia.util.ImageUrlUtil.getUrlForSize;

public class NewsLinkCard extends Card {
    @NonNull private CardPageItem page;
    @NonNull private Site site;

    public NewsLinkCard(@NonNull CardPageItem page, @NonNull Site site) {
        this.page = page;
        this.site = site;
    }

    @NonNull @Override public String title() {
        return page.title();
    }

    @Nullable @Override public String subtitle() {
        return page.description();
    }

    @Nullable @Override public Uri image() {
        Uri image = page.thumbnail();
        return image != null ? getUrlForSize(image, Constants.PREFERRED_THUMB_SIZE) : null;
    }

    @NonNull public PageTitle pageTitle() {
        PageTitle title = new PageTitle(page.title(), site);
        if (page.thumbnail() != null) {
            title.setThumbUrl(page.thumbnail().toString());
        }
        if (!TextUtils.isEmpty(page.description())) {
            title.setDescription(page.description());
        }
        return title;
    }
}
