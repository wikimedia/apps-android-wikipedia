package org.wikipedia.feed.image;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.UtcDate;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.DateUtil;

public class FeaturedImageCard extends Card {
    @NonNull private UtcDate date;
    @NonNull private Site site;
    @NonNull private FeaturedImage featuredImage;

    public FeaturedImageCard(@NonNull FeaturedImage featuredImage, @NonNull UtcDate date, @NonNull Site site) {
        this.featuredImage = featuredImage;
        this.site = site;
        this.date = date;
    }

    @NonNull
    public FeaturedImage baseImage() {
        return featuredImage;
    }

    @NonNull
    public UtcDate date() {
        return date;
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_featured_image_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(date.baseCalendar());
    }

    @NonNull
    public Site site() {
        return site;
    }

    @Override
    @NonNull
    public Uri image() {
        return featuredImage.thumbnail().source();
    }

    @Nullable
    public String description() {
        return featuredImage.description();
    }

    //Expose the language of the returned description in case we want to hide it if it doesn't match
    //the request Site language
    @Nullable
    public String descriptionLang() {
        return featuredImage.descriptionLang();
    }
}
