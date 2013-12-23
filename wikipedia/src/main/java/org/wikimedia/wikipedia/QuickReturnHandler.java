package org.wikimedia.wikipedia;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class QuickReturnHandler implements  ObservableWebView.OnScrollChangeListener {
    private final ObservableWebView webview;
    private final View quickReturnView;

    public QuickReturnHandler(ObservableWebView webview, View quickReturnView) {
        this.webview = webview;
        this.quickReturnView =  quickReturnView;

        webview.setOnScrollChangeListener(this);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        int animMargin;
        if (oldScrollY > scrollY) {
            int minMargin = 0;
            int scrollDelta = (oldScrollY - scrollY) / 2;
            int newMargin = (int)quickReturnView.getTranslationY() + scrollDelta;
            animMargin = Math.min(minMargin, newMargin);
        } else {
            // scroll downn!
            int minMargin = -quickReturnView.getHeight();
            int scrollDelta = (scrollY - oldScrollY) / 2;
            int newMargin = (int)quickReturnView.getTranslationY() - scrollDelta;
            animMargin = Math.max(minMargin, newMargin);
        }
        quickReturnView.setTranslationY(animMargin);
    }
}
