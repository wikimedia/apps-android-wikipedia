package org.wikipedia.feed.image;

import androidx.annotation.NonNull;

import org.wikipedia.gallery.GalleryItem;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.json.PostProcessingTypeAdapter;
import org.wikipedia.json.annotations.Required;

public final class FeaturedImage extends GalleryItem implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private String title;
    @SuppressWarnings("unused,NullableProblems") @Required @NonNull private ImageInfo image;

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
