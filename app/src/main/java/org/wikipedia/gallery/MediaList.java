package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class MediaList {
    @Nullable private String revision;
    @Nullable private String tid;
    @Nullable private List<MediaListItem> items;

    @NonNull
    public List<MediaListItem> getItems(@NonNull String... types) {
        List<MediaListItem> list = new ArrayList<>();
        if (items != null) {
            for (MediaListItem item : items) {
                if (item.showInGallery()) {
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
