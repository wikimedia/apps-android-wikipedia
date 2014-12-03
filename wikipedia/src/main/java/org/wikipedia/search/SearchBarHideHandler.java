package org.wikipedia.search;

import android.view.View;

import com.nineoldandroids.view.ViewHelper;

import org.wikipedia.ViewAnimations;
import org.wikipedia.views.ObservableWebView;

public class SearchBarHideHandler implements  ObservableWebView.OnScrollChangeListener, ObservableWebView.OnUpOrCancelMotionEventListener, ObservableWebView.OnDownMotionEventListener {
    private static final int HUMAN_SCROLL_THRESHOLD = 200;
    private final ObservableWebView webview;
    private final View quickReturnView;
    private final float displayDensity;

    public SearchBarHideHandler(ObservableWebView webview, View quickReturnView) {
        this.webview = webview;
        this.quickReturnView =  quickReturnView;
        this.displayDensity = quickReturnView.getResources().getDisplayMetrics().density;

        webview.addOnScrollChangeListener(this);
        webview.addOnDownMotionEventListener(this);
        webview.addOnUpOrCancelMotionEventListener(this);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (scrollY <= webview.getHeight()) {
            // For the first screenful, ensure it always exists.
            ViewAnimations.ensureTranslationY(quickReturnView, 0);
            return;
        }
        int animMargin;
        if (oldScrollY > scrollY) {
            int minMargin = 0;
            int scrollDelta = oldScrollY - scrollY;
            int newMargin = (int) ViewHelper.getTranslationY(quickReturnView) + scrollDelta;
            animMargin = Math.min(minMargin, newMargin);
        } else {
            // scroll down!
            int scrollDelta = scrollY - oldScrollY;
            if (scrollDelta > (int)(HUMAN_SCROLL_THRESHOLD * displayDensity)) {
                // we've been scrolled programmatically, probably to go to
                // a specific section, so keep the toolbar shown.
                animMargin = 0;
            } else {
                int minMargin = -quickReturnView.getHeight();
                int newMargin = (int) ViewHelper.getTranslationY(quickReturnView) - scrollDelta;
                animMargin = Math.max(minMargin, newMargin);
            }
        }
        ViewHelper.setTranslationY(quickReturnView, animMargin);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        int transY = (int)ViewHelper.getTranslationY(quickReturnView);
        int height = quickReturnView.getHeight();
        if (transY != 0 && transY > -height) {
            if (transY > -height / 2) {
                // Fully display it
                ViewAnimations.ensureTranslationY(quickReturnView, 0);
            } else {
                // Fully hide it
                ViewAnimations.ensureTranslationY(quickReturnView, -height);
            }
        }
    }

    @Override
    public void onDownMotionEvent() {
        // Don't do anything for now
    }
}
