package org.wikipedia.search;

import android.support.v7.app.ActionBarActivity;
import org.wikipedia.views.ObservableWebView;

public class SearchBarHideHandler implements  ObservableWebView.OnScrollChangeListener {
    private final ObservableWebView webview;
    private final ActionBarActivity activity;

    public SearchBarHideHandler(ObservableWebView webview, ActionBarActivity activity) {
        this.webview = webview;
        this.activity =  activity;

        webview.addOnScrollChangeListener(this);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (scrollY <= webview.getHeight()) {
            // For the first screenful, ensure it's always shown
            if (!activity.getSupportActionBar().isShowing()) {
                activity.getSupportActionBar().show();
            }
            return;
        }
        if (oldScrollY > scrollY) {
            if (!activity.getSupportActionBar().isShowing()) {
                activity.getSupportActionBar().show();
            }
        } else {
            if (activity.getSupportActionBar().isShowing()) {
                activity.getSupportActionBar().hide();
            }
        }
    }
}
