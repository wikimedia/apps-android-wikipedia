package org.wikipedia.feed.image;

import org.wikipedia.gallery.GalleryItem;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.json.annotations.Required;

import androidx.annotation.NonNull;

public final class FeaturedImage extends GalleryItem implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String title;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private ImageInfo image;

    private int age;

    public void setAge(int age) {
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    @NonNull
    public String title() {
        return title;
    }

    @Override
    public void postProcess() {
        setTitle(title);
        getOriginal().setSource(image.getSource());
    }
}
