package org.wikipedia.page.action;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;

import org.wikipedia.ViewAnimations;
import org.wikipedia.views.ObservableWebView;

public class PageActionToolbarHideHandler implements ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnUpOrCancelMotionEventListener,
        ObservableWebView.OnDownMotionEventListener {
    @NonNull private TabLayout pageActions;
    @Nullable private ObservableWebView webView;

    public PageActionToolbarHideHandler(@NonNull TabLayout pageActions) {
        this.pageActions = pageActions;
    }

    public void setScrollView(@Nullable ObservableWebView webView) {
        if (this.webView != null) {
            this.webView.clearAllListeners();
        }

        this.webView = webView;
        if (this.webView != null) {
            this.webView.addOnScrollChangeListener(this);
            this.webView.addOnDownMotionEventListener(this);
            this.webView.addOnUpOrCancelMotionEventListener(this);
        }
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        if (webView == null) {
            return;
        }

        if (scrollY <= webView.getHeight()) {
            // For the first screenful, ensure it always exists.
            ViewAnimations.ensureTranslationY(pageActions, 0);
            return;
        }

        int animMargin;
        int scrollDelta = scrollY - oldScrollY;
        int newMargin = (int) pageActions.getTranslationY() + scrollDelta;
        if (oldScrollY < scrollY) {
            // Scroll the page action toolbar down
            int minMargin = pageActions.getHeight();
            animMargin = Math.min(minMargin, newMargin);
        } else {
            // Scroll the page action toolbar up
            int minMargin = 0;
            animMargin = Math.max(minMargin, newMargin);
        }
        pageActions.setTranslationY(animMargin);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        int transY = (int) pageActions.getTranslationY();
        int height = pageActions.getHeight();
        if (transY != 0 && transY < height) {
            if (transY > height / 2) {
                // Fully hide the page action toolbar
                ViewAnimations.ensureTranslationY(pageActions, height);
            } else {
                // Fully display the page action toolbar
                ViewAnimations.ensureTranslationY(pageActions, 0);
            }
        }
    }

    @Override
    public void onDownMotionEvent() {
        // Don't do anything for now
    }
}
