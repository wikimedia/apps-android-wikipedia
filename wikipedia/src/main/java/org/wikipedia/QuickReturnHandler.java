package org.wikipedia;

import android.support.v7.app.ActionBarActivity;
import org.wikipedia.views.ObservableWebView;

public class QuickReturnHandler implements  ObservableWebView.OnScrollChangeListener {
    private final ObservableWebView webview;
    private final ActionBarActivity activity;

    public QuickReturnHandler(ObservableWebView webview, ActionBarActivity activity) {
        this.webview = webview;
        this.activity =  activity;

        webview.setOnScrollChangeListener(this);
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
