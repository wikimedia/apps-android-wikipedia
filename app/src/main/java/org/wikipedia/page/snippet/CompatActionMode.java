package org.wikipedia.page.snippet;

import android.os.Build;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.webkit.WebView;

import org.wikipedia.R;
import org.wikipedia.page.PageFragment;
import org.wikipedia.util.log.L;

import static org.wikipedia.views.ViewUtil.getOriginatingView;

public class CompatActionMode {

    private android.view.ActionMode actionMode;
    private ActionMode supportActionMode;
    private boolean isSupportActionMode;

    public <T> CompatActionMode(T mode) {
        if (mode instanceof ActionMode) {
            this.supportActionMode = (ActionMode) mode;
            this.isSupportActionMode = true;
        } else if (mode instanceof android.view.ActionMode) {
            this.actionMode = (android.view.ActionMode) mode;
            this.isSupportActionMode = false;
        } else {
            throw new IllegalArgumentException("Parameter must be of type android.view.ActionMode"
                    + " or android.support.v7.view.ActionMode");
        }
    }

    public boolean shouldInjectCustomMenu() {
        return !isTagged() && isOriginatedByWebViewOnMarshmallow();
    }

    public void injectCustomMenu(PageFragment fragment) {
        replaceTextSelectMenu();
        fragment.onActionModeShown(this);
    }

    public Menu getMenu() {
        return isSupportActionMode ? supportActionMode.getMenu() : actionMode.getMenu();
    }

    public void finish() {
        if (isSupportActionMode) {
            supportActionMode.finish();
        } else {
            actionMode.finish();
        }
    }

    private void replaceTextSelectMenu() {
        Menu menu = isSupportActionMode ? supportActionMode.getMenu() : actionMode.getMenu();
        menu.clear();
        MenuInflater inflater = isSupportActionMode ? supportActionMode.getMenuInflater() : actionMode.getMenuInflater();
        inflater.inflate(R.menu.menu_text_select, menu);
    }

    private boolean isTagged() {
        return isSupportActionMode ? supportActionMode.getTag() != null : actionMode.getTag() != null;
    }

    /**
     * When the client is running Marshmallow or higher, check to ensure that the action mode was
     * originated by the WebView before injecting the custom CAB menu.
     * @return true if originated by a WebView.
     */
    private boolean isOriginatedByWebViewOnMarshmallow() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || isOriginatedByWebView();
    }

    private boolean isOriginatedByWebView() {
        boolean isOriginatedByWebView = false;
        try {
            isOriginatedByWebView = getOriginatingView(actionMode) instanceof WebView;
        } catch (Throwable caught) {
            L.w("Caught " + caught.getMessage(), caught);
        }
        return isOriginatedByWebView;
    }
}
