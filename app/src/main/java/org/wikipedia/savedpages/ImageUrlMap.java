package org.wikipedia.savedpages;

import android.graphics.drawable.Drawable;
import android.support.annotation.VisibleForTesting;
import android.text.Html;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.Page;
import org.wikipedia.page.Section;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.wikipedia.util.StringUtil.md5string;

/**
 * Mappings of image source URLs to local file URLs.
 * A Map with entries {source URL, file path} of images to be downloaded.
 */
public final class ImageUrlMap {
    private final Map<String, String> urlMap;

    private ImageUrlMap(Builder builder) {
        // Mostly since multiple threads can attempt to remove things at the same time
        // in SavePageTask, if multiple images fail. Easier than synchronizing just the removes.
        this.urlMap = Collections.synchronizedMap(builder.urlMap);
    }

    public Iterable<? extends Map.Entry<String, String>> entrySet() {
        return urlMap.entrySet();
    }

    public int size() {
        return urlMap.size();
    }

    /** Call this if an image failed to be saved */
    public void remove(String url) {
        urlMap.remove(url);
    }

    /**
     * Creates a JSON representation which can be used on the JS side.
     * See replaceImageSrc in main.js and #replaceImgUrls.
     *
     * @return a JSONObject of all mappings: "originalURL" -> "newURL"
     */
    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            JSONArray mapJSON = new JSONArray();
            for (Map.Entry<String, String> entry : urlMap.entrySet()) {
                JSONObject entryJSON = new JSONObject();
                entryJSON.put("originalURL", entry.getKey());
                entryJSON.put("newURL", makeFileUrl(entry.getValue()));
                mapJSON.put(entryJSON);
            }
            json.put("img_map", mapJSON);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Triggers img src URL replacements based on the map found in JSON (when loading a saved page)
     * @param bridge the link to the JS world
     * @param urlMapJSON should be same JSON format that was created by #toJSON.
     */
    public static void replaceImageSources(CommunicationBridge bridge, JSONObject urlMapJSON) {
        bridge.sendMessage("replaceImageSources", urlMapJSON);
    }

    private String makeFileUrl(String fileString) {
        return "file://" + fileString;
    }


    /**
     * Needed to create an immutable ImageUrlMap.
     */
    public static class Builder {
        private final String imgDir;
        private final Map<String, String> urlMap = new LinkedHashMap<>();

        public Builder(String imgDir) {
            this.imgDir = fixImgDir(imgDir);
        }

        /** Makes sure the directory string looks reasonable: starts and ends with a slash. */
        private String fixImgDir(String imgDir) {
            if (!imgDir.startsWith("/")) {
                throw new IllegalArgumentException(imgDir + " must start with a /");
            }
            if (!imgDir.endsWith("/")) {
                imgDir = imgDir + "/";
            }
            return imgDir;
        }

        /**
         * Extracts all img src URLs, section by section.
         *
         * @param page the input page
         */
        public Builder extractUrls(Page page) {
            for (Section section : page.getSections()) {
                extractUrlsInSection(section.getContent());
            }
            return this;
        }

        /**
         * Extracts image source URLs from input HTML in one section.
         *
         * @param html the input HTML string
         */
        @VisibleForTesting
        public void extractUrlsInSection(String html) {
            Html.fromHtml(html, imageGetter, null);
        }

        /** Custom ImageGetter which collects all img src URLs. */
        private Html.ImageGetter imageGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String url) {
                urlMap.put(url, urlToFilePath(url));
                return null;
            }
        };

        private String urlToFilePath(String url) {
            return imgDir + md5string(url) + getFileExtension(url);
        }

        private String getFileExtension(String url) {
            int index = url.lastIndexOf(".");
            return url.substring(index);
        }

        public ImageUrlMap build() {
            return new ImageUrlMap(this);
        }
    }
}
