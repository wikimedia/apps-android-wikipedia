package org.wikipedia.bridge;

import org.json.*;

/**
 * Represents a bundle of CSS files that can be loaded into a
 * webview via a CommunicationBridge
 */
public abstract class StyleBundle {
    /**
     * Array containing full path of the CSS files in this
     * bundle.
     */
    private final String stylePaths[];

    /**
     * Creates a new StyleBundle with a styles from a common prefix.
     *
     * @param prefix Prefix (with trailing slash) to use for all CSS files
 *                   in this bundle.
     * @param styles Array of CSS File names that are available together in
     *               the prefix.
     */
    public StyleBundle(String prefix, String... styles) {
        stylePaths = new String[styles.length];
        for (int i = 0; i < styles.length; i++) {
            stylePaths[i] = prefix + styles[i];
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
            // This never happens
            throw new RuntimeException(e);
        }
    }
}
