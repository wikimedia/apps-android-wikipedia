package org.wikipedia.page.snippet;

import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageProperties;

/**
 * Allows sharing selected text in the WebView.
 */
public class TextSelectedShareAdapter {
    private final PageActivity activity;
    private final WikipediaApp app;
    private ActionMode webViewActionMode;
    private static ClipboardManager.OnPrimaryClipChangedListener CLIP_LISTENER;
    private MenuItem copyMenuItem;
    private MenuItem shareItem;
    private ShareAFactFunnel funnel;

    public TextSelectedShareAdapter(PageActivity activity) {
        this.activity = activity;
        app = (WikipediaApp) activity.getApplicationContext();
    }

    public void finish() {
        if (webViewActionMode != null) {
            webViewActionMode.finish();
        }
    }

    /**
     * Since API <11 doesn't provide a long-press context for the WebView anyway, and we're
     * using clipboard features that are only supported in API 11+, we'll mark this whole
     * method as TargetApi(11), so that the IDE doesn't get upset.
     * @param mode ActionMode under which this context is starting.
     */
    @TargetApi(11)
    public void onTextSelected(final ActionMode mode) {
        webViewActionMode = mode;
        Menu menu = mode.getMenu();

        // Find the context menu item for copying text to the clipboard...
        // The most practical way to do this seems to be to get the resource name of the
        // menu item, and see if it resembles "action_menu_copy", which appears to remain
        // consistent throughout the various APIs.
        for (int i = 0; i < menu.size(); i++) {
            String resourceName
                    = activity.getResources().getResourceName(menu.getItem(i).getItemId());
            if (resourceName.contains("action_menu_copy")) {
                copyMenuItem = menu.getItem(i);
                break;
            }
        }

        // Find the context menu item for sharing text...
        // The most practical way to do this seems to be to get the resource name of the
        // menu item, and see if it resembles "action_menu_share", which appears to remain
        // consistent throughout the various APIs.
        for (int i = 0; i < menu.size(); i++) {
            String resourceName
                    = activity.getResources().getResourceName(menu.getItem(i).getItemId());
            if (resourceName.contains("action_menu_share")) {
                shareItem = menu.getItem(i);
                break;
            }
        }

        // add our clipboard listener, so that we'll get an event when the text
        // is copied onto it...
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                Context.CLIPBOARD_SERVICE);
        if (CLIP_LISTENER == null) {
            CLIP_LISTENER = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    // get the text from the clipboard!
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                            Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()
                            && clipboard.getPrimaryClip().getItemCount() > 0) {

                        CharSequence selectedText = clipboard.getPrimaryClip().getItemAt(0)
                                .coerceToText(activity);
                        Log.d("Share", ">>> Clipboard text: " + selectedText);

                        // Pass the clipboard text to a new Share handler!
                        new ShareHandler(activity, funnel).shareSnippet(selectedText, false);
                    }
                    clipboard.removePrimaryClipChangedListener(CLIP_LISTENER);
                }
            };
        }
        // remove it first, just in case it was added from the last context, and
        // ended up not being used.
        clipboard.removePrimaryClipChangedListener(CLIP_LISTENER);
        // and add it again.
        clipboard.addPrimaryClipChangedListener(CLIP_LISTENER);

        // intercept share menu...
        shareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (copyMenuItem != null) {
                    // programmatically invoke the copy-to-clipboard action...
                    webViewActionMode.getMenu()
                            .performIdentifierAction(copyMenuItem.getItemId(), 0);
                    // this will trigger a state-change event in the Clipboard, which we'll
                    // catch with our listener above.
                }
                // leave context mode...
                if (webViewActionMode != null) {
                    webViewActionMode.finish();
                }
                return true;
            }
        });

        final Page page = activity.getCurPageFragment().getPage();
        final PageProperties pageProperties = page.getPageProperties();
        funnel = new ShareAFactFunnel(app, page.getTitle(), pageProperties.getPageId(),
                pageProperties.getRevisionId());
        funnel.logHighlight();
    }
}
