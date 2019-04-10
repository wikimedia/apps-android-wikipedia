package org.wikipedia.gallery;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Gallery {
    @SuppressWarnings("unused,NullableProblems") @Nullable private String revision;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String tid;
    @SuppressWarnings("unused") @Nullable private List<GalleryItem> items;

    @Nullable
    public List<GalleryItem> getAllItems() {
        return items;
    }

    @NonNull
    public List<GalleryItem> getItems(@NonNull String... types) {
        List<GalleryItem> list = new ArrayList<>();
        if (items != null) {
            for (GalleryItem item : items) {
                if (item.isShowInGallery()) {
                    for (String type : types) {
                        if (item.getType().contains(type)) {
                            list.add(item);
                        }
                    }
                }
            }
        }
        return list;
    }
}
