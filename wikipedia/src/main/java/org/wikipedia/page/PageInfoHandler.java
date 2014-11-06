package org.wikipedia.page;

import org.wikipedia.Utils;
import org.wikipedia.bridge.CommunicationBridge;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.text.Spannable;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * A handler for both disambiguation and page issues information.
 * It listens to disambig and page issues link clicks from the WebView.
 * When clicked it shows the PageInfoDialog with the respective list.
 */
abstract class PageInfoHandler implements CommunicationBridge.JSEventListener {
    private final Activity activity;
    private PageInfoDialog dialog;

    PageInfoHandler(Activity activity, CommunicationBridge bridge) {
        this.activity = activity;
        bridge.addListener("disambigClicked", this);
        bridge.addListener("issuesClicked", this);
    }

    private LinkMovementMethodExt movementMethod = new LinkMovementMethodExt(getLinkHandler()) {
        @Override
        public boolean onTouchEvent(final TextView widget, final Spannable buffer, final MotionEvent event) {
            boolean ret = super.onTouchEvent(widget, buffer, event);
            if (ret && event.getAction() == MotionEvent.ACTION_UP) {
                dialog.dismiss();
            }
            return ret;
        }
    };

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            PageInfo info = new PageInfo(Utils.jsonArrayToStringArray(messagePayload.getJSONArray("hatnotes")),
                                         Utils.jsonArrayToStringArray(messagePayload.getJSONArray("issues")));
            dialog = new PageInfoDialog(activity, info, getDialogHeight(), movementMethod);
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

    abstract LinkHandler getLinkHandler();

    abstract int getDialogHeight();
}
