package org.wikipedia.feed.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.Site;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.StringUtil;

public abstract class BigPictureCard extends Card {
    @NonNull private String articleTitle;
    @Nullable private String articleSubtitle;
    @Nullable private String extract;
    @Nullable private Uri image;
    @NonNull private UtcDate age;
    @NonNull private Site site;

    public BigPictureCard(@NonNull CardPageItem page, @NonNull UtcDate age, @NonNull Site site) {
        this.articleTitle = page.title();
        this.articleSubtitle = page.description() != null
                ? StringUtil.capitalizeFirstChar(page.description()) : null;
        this.image = page.thumbnail();
        this.extract = page.extract();
        this.age = age;
        this.site = site;
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(age.baseCalendar());
    }

    @NonNull
    public String articleTitle() {
        return articleTitle;
    }

    @Nullable
    public String articleSubtitle() {
        return articleSubtitle;
    }

    @Override
    @Nullable
    public Uri image() {
        return image;
    }

    @Nullable
    @Override
    public String extract() {
        return extract;
    }

    @NonNull
    public PageTitle pageTitle() {
        PageTitle title = new PageTitle(articleTitle, site);
        if (image != null) {
            title.setThumbUrl(image.toString());
        }
        if (!TextUtils.isEmpty(articleSubtitle)) {
            title.setDescription(articleSubtitle);
        }
        return title;
    }
}
