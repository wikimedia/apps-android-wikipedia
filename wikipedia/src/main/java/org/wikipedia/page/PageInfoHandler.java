package org.wikipedia.page;

import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.bridge.CommunicationBridge;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A handler for both disambiguation and page issues information.
 * It listens to disambig and page issues link clicks from the WebView.
 * When clicked it shows the PageInfoDialog with the respective list.
 */
abstract class PageInfoHandler implements CommunicationBridge.JSEventListener {
    private final PageActivity activity;
    private final Site site;

    PageInfoHandler(PageActivity activity, CommunicationBridge bridge, Site site) {
        this.activity = activity;
        this.site = site;
        bridge.addListener("disambigClicked", this);
        bridge.addListener("issuesClicked", this);
    }

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            PageInfo info = new PageInfo(parseDisambigJson(messagePayload.getJSONArray("hatnotes")),
                                         Utils.jsonArrayToStringArray(messagePayload.getJSONArray("issues")));
            PageInfoDialog dialog = new PageInfoDialog(activity, info, getDialogHeight());
            dialog.show();
            if ("disambigClicked".equals(messageType)) {
                dialog.showDisambig();
            } else if ("issuesClicked".equals(messageType)) {
                dialog.showIssues();
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private DisambigResult[] parseDisambigJson(JSONArray array) throws JSONException {
        if (array == null) {
            return null;
        }
        DisambigResult[] stringArray = new DisambigResult[array.length()];
        for (int i = 0; i < array.length(); i++) {
            stringArray[i] = new DisambigResult(new PageTitle(array.getString(i), site));
        }
        return stringArray;
    }

    abstract int getDialogHeight();
}
