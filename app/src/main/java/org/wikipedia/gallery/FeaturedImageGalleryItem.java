package org.wikipedia.gallery;

import android.support.annotation.NonNull;

import org.wikipedia.feed.image.FeaturedImage;

public class FeaturedImageGalleryItem extends GalleryItem {

    private int age;

    public FeaturedImageGalleryItem(@NonNull FeaturedImage featuredImage, int age) {
        super(featuredImage.title());
        this.age = age;
        this.getOriginal().setSource(featuredImage.image().source().toString());
        this.getThumbnail().setSource(featuredImage.thumbnail().source().toString());
        this.getDescription().setHtml(featuredImage.description());

        // TODO: we can fetch image license by using the ImageLicenseFetchClient
    }

    public int getAge() {
        return age;
    }
}
