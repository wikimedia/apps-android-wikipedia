package org.wikipedia;

import android.view.*;
import com.nineoldandroids.view.ViewHelper;

public class QuickReturnHandler implements  ObservableWebView.OnScrollChangeListener, ObservableWebView.OnUpOrCancelMotionEventListener, ObservableWebView.OnDownMotionEventListener {
    private final ObservableWebView webview;
    private final View quickReturnView;

    public QuickReturnHandler(ObservableWebView webview, View quickReturnView) {
        this.webview = webview;
        this.quickReturnView =  quickReturnView;

        webview.setOnScrollChangeListener(this);
        webview.setOnDownMotionEventListener(this);
        webview.setOnUpOrCancelMotionEventListener(this);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (scrollY <= webview.getHeight()) {
            // For the first screenful, ensure it always exists.
            Utils.ensureTranslationY(quickReturnView, 0);
            return;
        }
        int animMargin;
        if (oldScrollY > scrollY) {
            int minMargin = 0;
            int scrollDelta = oldScrollY - scrollY;
            int newMargin = (int) ViewHelper.getTranslationY(quickReturnView) + scrollDelta;
            animMargin = Math.min(minMargin, newMargin);
        } else {
            // scroll downn!
            int minMargin = -quickReturnView.getHeight();
            int scrollDelta = scrollY - oldScrollY;
            int newMargin = (int)ViewHelper.getTranslationY(quickReturnView) - scrollDelta;
            animMargin = Math.max(minMargin, newMargin);
        }
        ViewHelper.setTranslationY(quickReturnView, animMargin);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        int transY = (int)ViewHelper.getTranslationY(quickReturnView);
        int height = quickReturnView.getHeight();
        if (transY != 0 && transY > -height) {
            if (transY > -height / 2 ) {
                // Fully display it
                Utils.ensureTranslationY(quickReturnView, 0);
            } else {
                // Fully hide it
                Utils.ensureTranslationY(quickReturnView, -height);
            }
        }
    }

    @Override
    public void onDownMotionEvent() {
        // Don't do anything for now
    }
}
