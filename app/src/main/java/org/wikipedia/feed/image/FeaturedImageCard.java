package org.wikipedia.feed.image;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.L10nUtil;

public class FeaturedImageCard extends WikiSiteCard {
    @NonNull private FeaturedImage featuredImage;
    private int age;

    public FeaturedImageCard(@NonNull FeaturedImage featuredImage, int age, @NonNull WikiSite wiki) {
        super(wiki);
        this.featuredImage = featuredImage;
        this.age = age;
    }

    @NonNull
    public FeaturedImage baseImage() {
        return featuredImage;
    }

    public int age() {
        return age;
    }

    @Override
    @NonNull
    public String title() {
        return L10nUtil.getStringForArticleLanguage(wikiSite().languageCode(), R.string.view_featured_image_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return DateUtil.getFeedCardDateString(age);
    }

    @NonNull
    public String filename() {
        return featuredImage.title();
    }

    @Override
    @NonNull
    public Uri image() {
        return Uri.parse(featuredImage.getThumbnailUrl());
    }

    @NonNull @Override public CardType type() {
        return CardType.FEATURED_IMAGE;
    }

    @Nullable
    public String description() {
        return featuredImage.getDescription().getText();
    }

    @Override
    protected int dismissHashCode() {
        return featuredImage.title().hashCode();
    }
}
