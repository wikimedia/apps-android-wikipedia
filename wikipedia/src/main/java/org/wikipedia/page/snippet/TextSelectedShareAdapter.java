package org.wikipedia.page.snippet;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageActivity;
import org.wikipedia.util.ApiUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Allows sharing selected text in the WebView.
 */
public class TextSelectedShareAdapter extends ShareHandler {
    private ActionMode webViewActionMode;
    private CommunicationBridge bridge;

    public TextSelectedShareAdapter(PageActivity parentActivity, CommunicationBridge bridge) {
        super(parentActivity);
        this.bridge = bridge;

        bridge.addListener("onGetTextSelection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String purpose = messagePayload.getString("purpose");
                    String text = messagePayload.getString("text");
                    if (purpose.equals("share")) {
                        shareSnippet(text, false);
                        getFunnel().logShareTap(text);
                    }
                } catch (JSONException e) {
                    //nope
                }
            }
        });
    }

    /**
     * @param mode ActionMode under which this context is starting.
     */
    public void onTextSelected(ActionMode mode) {
        MenuItem shareItem = null;
        webViewActionMode = mode;
        Menu menu = mode.getMenu();

        // Find the context menu items from the WebView's action mode.
        // The most practical way to do this seems to be to get the resource name of the
        // menu item, and see if it contains "copy", "share", etc. which appears to remain
        // consistent throughout the various APIs.
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            try {
                String resourceName = getActivity().getResources().getResourceName(item.getItemId());
                if (resourceName.contains("share")) {
                    shareItem = item;
                }
                // In APIs lower than 21, some of the action mode icons may not respect the
                // current theme, so we need to manually tint those icons.
                if (!ApiUtil.hasLollipop()) {
                    fixMenuItemTheme(item);
                }
            } catch (Resources.NotFoundException e) {
                // Looks like some devices don't provide access to these menu items through
                // the context of the app, in which case, there's nothing we can do...
            }
        }

        // if we were unable to find the Share button, then inject our own!
        if (shareItem == null) {
            shareItem = mode.getMenu().add(Menu.NONE, Menu.NONE, Menu.NONE,
                                           getActivity().getString(R.string.menu_share_page));
            shareItem.setIcon(R.drawable.ic_share_dark);
            MenuItemCompat.setShowAsAction(shareItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS
                                                      | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

        // provide our own listener for the Share button...
        shareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // send an event to the WebView that will make it return the
                // selected text back to us...
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("purpose", "share");
                    bridge.sendMessage("getTextSelection", payload);
                } catch (JSONException e) {
                    //nope
                }

                // leave context mode...
                if (webViewActionMode != null) {
                    webViewActionMode.finish();
                    webViewActionMode = null;
                }
                return true;
            }
        });

        createFunnel();
        getFunnel().logHighlight();
    }

    private void fixMenuItemTheme(MenuItem item) {
        if (item != null && item.getIcon() != null) {
            WikipediaApp.getInstance().setDrawableTint(item.getIcon(), Color.WHITE);
        }
    }
}
