package org.wikipedia.util;

import org.json.JSONArray;

public final class JsonUtil {

    /**
     * Convert a JSONArray object to a String Array.
     *
     * @param array a JSONArray containing only Strings
     * @return a String[] with all the items in the JSONArray
     */
    public static String[] jsonArrayToStringArray(JSONArray array) {
        if (array == null) {
            return null;
        }
        String[] stringArray = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            stringArray[i] = array.optString(i);
        }
        return stringArray;
    }

    private JsonUtil() {

    }
}
