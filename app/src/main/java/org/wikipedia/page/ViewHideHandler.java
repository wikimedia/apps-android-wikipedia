package org.wikipedia.page;

import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewAnimations;

public class ViewHideHandler
        implements ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnUpOrCancelMotionEventListener,
        ObservableWebView.OnDownMotionEventListener, ObservableWebView.OnClickListener {
    @NonNull private final View hideableView;
    @Nullable private final View anchoredView;
    @Nullable private ObservableWebView webView;
    private final int gravity;

    public ViewHideHandler(@NonNull View hideableView, @Nullable View anchoredView, int gravity) {
        this.hideableView = hideableView;
        this.anchoredView = anchoredView;
        this.gravity = gravity;
    }

    /**
     * Update the WebView based on whose scroll position the search bar will hide itself.
     * @param webView The WebView against which scrolling will be tracked.
     */
    public void setScrollView(@Nullable ObservableWebView webView) {
        this.webView = webView;
        if (webView != null) {
            webView.addOnScrollChangeListener(this);
            webView.addOnDownMotionEventListener(this);
            webView.addOnUpOrCancelMotionEventListener(this);
            webView.addOnClickListener(this);
        }
    }

    /**
     * Force an update of the appearance of the search bar. Usually it is updated automatically
     * when the associated WebView is scrolled, but this function may be used to manually refresh
     * the appearance of the search bar, e.g. when the WebView is first shown.
     */
    public void update() {
        if (webView == null) {
            return;
        }
        onScrollChanged(webView.getScrollY(), webView.getScrollY(), false);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        if (webView == null) {
            return;
        }

        int animMargin = 0;
        int scrollDelta = scrollY - oldScrollY;
        if (gravity == Gravity.BOTTOM) {
            if (oldScrollY < scrollY) {
                // Scroll down
                animMargin = Math.min(hideableView.getHeight(),
                        (int) hideableView.getTranslationY() + scrollDelta);
            } else {
                // Scroll up
                animMargin = Math.max(0, (int) hideableView.getTranslationY() + scrollDelta);
            }
        } else if (gravity == Gravity.TOP) {
            if (oldScrollY > scrollY) {
                // scroll up
                animMargin = Math.min(0, (int) hideableView.getTranslationY() - scrollDelta);
            } else {
                // scroll down
                animMargin = Math.max(-hideableView.getHeight(),
                        (int) hideableView.getTranslationY() - scrollDelta);
            }
        }
        hideableView.setTranslationY(animMargin);
        if (anchoredView != null) {
            anchoredView.setTranslationY(animMargin);
        }

        float elevation = scrollY == 0 && oldScrollY != 0 ? 0 : DimenUtil.dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation));
        if (elevation != hideableView.getElevation()) {
            hideableView.setElevation(elevation);
        }
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        int transY = (int) hideableView.getTranslationY();
        int height = hideableView.getHeight();
        if (gravity == Gravity.BOTTOM && transY != 0 && transY < height) {
            if (transY > height / 2) {
                ensureHidden();
            } else {
                ensureDisplayed();
            }
        } else if (gravity == Gravity.TOP && transY != 0 && transY > -height) {
            if (transY > -height / 2) {
                ensureDisplayed();
            } else {
                ensureHidden();
            }
        }
    }

    @Override
    public void onDownMotionEvent() {
        // Don't do anything for now
    }

    @Override
    public boolean onClick(float x, float y) {
        ensureDisplayed();
        return false;
    }

    private void ensureDisplayed() {
        ViewAnimations.ensureTranslationY(hideableView, 0);
        if (anchoredView != null) {
            ViewAnimations.ensureTranslationY(anchoredView, 0);
        }
    }

    private void ensureHidden() {
        int translation = gravity == Gravity.BOTTOM ? hideableView.getHeight() : -hideableView.getHeight();
        ViewAnimations.ensureTranslationY(hideableView, translation);
        if (anchoredView != null) {
            ViewAnimations.ensureTranslationY(anchoredView, translation);
        }
    }
}
