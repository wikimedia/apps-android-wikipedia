package org.wikimedia.wikipedia;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

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
            if (quickReturnView.getTranslationY() != 0) {
                quickReturnView.animate().translationY(0).setDuration(WikipediaApp.SHORT_ANIMATION_DURATION).start();
            }
            return;
        }
        int animMargin;
        if (oldScrollY > scrollY) {
            int minMargin = 0;
            int scrollDelta = oldScrollY - scrollY;
            int newMargin = (int)quickReturnView.getTranslationY() + scrollDelta;
            animMargin = Math.min(minMargin, newMargin);
        } else {
            // scroll downn!
            int minMargin = -quickReturnView.getHeight();
            int scrollDelta = scrollY - oldScrollY;
            int newMargin = (int)quickReturnView.getTranslationY() - scrollDelta;
            animMargin = Math.max(minMargin, newMargin);
        }
        quickReturnView.setTranslationY(animMargin);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        int transY = (int)quickReturnView.getTranslationY();
        int height = quickReturnView.getHeight();
        if (transY != 0 && transY > -height) {
            if (transY > -height / 2 ) {
                // Fully display it
                quickReturnView.animate().translationY(0).setDuration(WikipediaApp.SHORT_ANIMATION_DURATION).start();
            } else {
                // Fully hide it
                quickReturnView.animate().translationY(-height).setDuration(WikipediaApp.SHORT_ANIMATION_DURATION).start();
            }
        }
    }

    @Override
    public void onDownMotionEvent() {
        // Don't do anything for now
    }
}
