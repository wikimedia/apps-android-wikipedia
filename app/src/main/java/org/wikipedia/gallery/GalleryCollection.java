package org.wikipedia.gallery;

import android.support.annotation.NonNull;

import java.util.List;

public class GalleryCollection {
    private static final int MIN_IMAGE_SIZE = 64;

    @NonNull private List<GalleryItem> itemList;

    @NonNull public List<GalleryItem> getItemList() {
        return itemList;
    }

    public GalleryCollection(@NonNull List<GalleryItem> list) {
        this.itemList = list;
    }

    public static boolean shouldIncludeImage(@NonNull ImageInfo info) {
        return !isTooSmall(info) && !isWrongMimeType(info);
    }

    private static boolean isTooSmall(@NonNull ImageInfo info) {
        return info.getWidth() < MIN_IMAGE_SIZE || info.getHeight() < MIN_IMAGE_SIZE;
    }

    private static boolean isWrongMimeType(@NonNull ImageInfo info) {
        return info.getMimeType() != null
                && (info.getMimeType().contains("svg") || info.getMimeType().contains("png"));
    }
}
