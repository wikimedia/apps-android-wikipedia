package org.wikipedia.page.gallery;

import org.wikipedia.PageTitle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GalleryCollection {
    private static final int MIN_IMAGE_SIZE = 64;

    private List<GalleryItem> itemList;
    public List<GalleryItem> getItemList() {
        return itemList;
    }

    public GalleryCollection(Map<PageTitle, GalleryItem> galleryMap) {
        itemList = new ArrayList<>();
        Iterator iterator = galleryMap.keySet().iterator();
        while (iterator.hasNext()) {
            GalleryItem item = galleryMap.get(iterator.next());
            if (item.getWidth() < MIN_IMAGE_SIZE || item.getHeight() < MIN_IMAGE_SIZE) {
                // reject gallery items if they're too small
                continue;
            } else if (item.getMimeType().contains("svg") || item.getMimeType().contains("png")) {
                // also reject SVG and PNG items by default, because they're likely to be
                // logos and/or presentational images
                continue;
            }
            itemList.add(item);
        }
    }
}
