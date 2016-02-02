package org.wikipedia.bridge;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a bundle of CSS files that can be loaded into a
 * webview via a CommunicationBridge
 */
public class StyleBundle {
    public static final String BUNDLE_PAGEVIEW = "styles.css";
    public static final String BUNDLE_PREVIEW = "preview.css";
    public static final String BUNDLE_NIGHT_MODE = "night.css";

    /**
     * Array containing full path of the CSS files in this
     * bundle.
     */
    private final String[] stylePaths;

    /**
     * Returns a bundle of styles of a specific type.
     * @return Requested style bundle.
     */
    public static StyleBundle getAvailableBundle(String type) {
        return new StyleBundle(type);
    }

    /**
     * Creates a new StyleBundle with a styles from a common prefix.
     *
     * @param styles Array of CSS File names that are available together in
     *               the prefix.
     */
    public StyleBundle(String... styles) {
        stylePaths = new String[styles.length];
        for (int i = 0; i < styles.length; i++) {
            stylePaths[i] = "file:///android_asset/" + styles[i];
        }
    }

    /**
     * Return a JSON encoded version of this bundle.
     *
     * @return A JSONObject which fully encodes the data in this bundle.
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONArray stylesJSON = new JSONArray();
        for (String stylePath : stylePaths) {
            stylesJSON.put(stylePath);
        }
        try {
            json.put("style_paths", stylesJSON);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
