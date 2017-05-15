package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GalleryCollection {
    private static final int MIN_IMAGE_SIZE = 64;

    @NonNull private List<GalleryItem> itemList;

    @NonNull public List<GalleryItem> getItemList() {
        return itemList;
    }

    @VisibleForTesting public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            JSONArray itemsJSON = new JSONArray();
            for (GalleryItem item : itemList) {
                JSONObject itemJSON = item.toJSON();
                if (itemJSON != null) {
                    itemsJSON.put(itemJSON);
                }
            }
            json.put("items", itemsJSON);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public GalleryCollection(@NonNull List<GalleryItem> list) {
        this.itemList = list;
    }

    public GalleryCollection(@NonNull JSONObject json) {
        itemList = new ArrayList<>();
        try {
            JSONArray itemsJSON = json.getJSONArray("items");
            for (int i = 0; i < itemsJSON.length(); i++) {
                JSONObject itemJSON = itemsJSON.getJSONObject(i);
                itemList.add(new GalleryItem(itemJSON));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
