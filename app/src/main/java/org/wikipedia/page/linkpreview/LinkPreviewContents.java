package org.wikipedia.page.linkpreview;

import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Utils;

public class LinkPreviewContents {

    private final PageTitle title;
    public PageTitle getTitle() {
        return title;
    }

    private final String leadImageName;
    public String getLeadImageName() {
        return leadImageName;
    }

    private final String extract;
    public String getExtract() {
        return extract;
    }

    public LinkPreviewContents(JSONObject json, Site site) throws JSONException {
        title = new PageTitle(json.getString("title"), site);
        // replace newlines in the extract with double newlines, so that they'll show up
        // as paragraph breaks when displayed in a TextView.
        extract = json.getString("extract").replace("\n", "\n\n");
        if (json.has("thumbnail")) {
            title.setThumbUrl(json.getJSONObject("thumbnail").optString("source"));
        }
        if (json.has("terms") && json.getJSONObject("terms").has("description")) {
            title.setDescription(Utils.capitalizeFirstChar(json.getJSONObject("terms").getJSONArray("description").optString(0)));
        }
        leadImageName = json.has("pageimage") ? "File:" + json.optString("pageimage") : null;
    }

    /**
     * Remove text contained in parentheses from a string.
     * @param text String to be processed.
     * @return New string that is the same as the original string, but without any
     * content in parentheses.
     */
    public static String removeParens(String text) {
        StringBuilder outStr = new StringBuilder(text.length());
        char c;
        int level = 0;
        int i = 0;
        for (; i < text.length(); i++) {
            c = text.charAt(i);
            if (c == ')' && level == 0) {
                // abort if we have an imbalance of parentheses
                return text;
            }
            if (c == '(') {
                level++;
                continue;
            } else if (c == ')') {
                level--;
                continue;
            }
            if (level == 0) {
                // Remove leading spaces before parentheses
                if (c == ' ' && (i < text.length() - 1) && text.charAt(i + 1) == '(') {
                    continue;
                }
                outStr.append(c);
            }
        }
        // fill in the rest of the string
        if (i + 1 < text.length()) {
            outStr.append(text.substring(i + 1, text.length()));
        }
        // if we had an imbalance of parentheses, then return the original string,
        // instead of the transformed one.
        return (level == 0) ? outStr.toString() : text;
    }

}
