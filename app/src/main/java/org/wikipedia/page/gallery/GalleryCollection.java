package org.wikipedia.page.gallery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GalleryCollection {
    private static final int MIN_IMAGE_SIZE = 64;

    private List<GalleryItem> itemList;
    public List<GalleryItem> getItemList() {
        return itemList;
    }

    public JSONObject toJSON() {
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

    public GalleryCollection(JSONObject json) {
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

    public GalleryCollection(Map<PageTitle, GalleryItem> galleryMap) {
        itemList = new ArrayList<>();
        for (PageTitle title : galleryMap.keySet()) {
            GalleryItem item = galleryMap.get(title);
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
