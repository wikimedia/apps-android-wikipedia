package org.wikipedia.gallery;

import android.support.annotation.NonNull;

import org.wikipedia.feed.image.FeaturedImage;

class FeaturedImageGalleryItem extends GalleryItem {

    private int age;

    FeaturedImageGalleryItem(@NonNull FeaturedImage featuredImage, int age) {
        super(featuredImage.title());
        this.age = age;
        setUrl(featuredImage.image().source().toString());
        setWidth(featuredImage.image().width());
        setHeight(featuredImage.image().height());
        setThumbUrl(featuredImage.thumbnail().source().toString());
        setWildcardMimeType();
        setPlaceHolderLicense();
    }

    public int getAge() {
        return age;
    }

    private void setWildcardMimeType() {
        setMimeType("*/*");
    }

    private void setPlaceHolderLicense() {
        setLicense(new ImageLicense());
    }
}
