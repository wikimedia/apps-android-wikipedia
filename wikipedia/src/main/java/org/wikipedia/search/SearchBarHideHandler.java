package org.wikipedia.search;

import android.os.Build;
import android.view.View;

import com.nineoldandroids.view.ViewHelper;

import org.wikipedia.R;
import org.wikipedia.ViewAnimations;
import org.wikipedia.views.ObservableWebView;

public class SearchBarHideHandler implements ObservableWebView.OnScrollChangeListener, ObservableWebView.OnUpOrCancelMotionEventListener, ObservableWebView.OnDownMotionEventListener {
    private static final int HUMAN_SCROLL_THRESHOLD = 200;
    private final View quickReturnView;
    private final float displayDensity;
    private ObservableWebView webview;
    private boolean fadeEnabled = false;
    private boolean forceNoFade = false;
    private View toolbarBackground;
    private View toolbarSearchBackground;
    private View toolbarGradient;
    private View toolbarShadow;

    public SearchBarHideHandler(View quickReturnView) {
        this.quickReturnView =  quickReturnView;
        this.displayDensity = quickReturnView.getResources().getDisplayMetrics().density;

        toolbarBackground = quickReturnView.findViewById(R.id.main_toolbar_background);
        toolbarSearchBackground = quickReturnView.findViewById(R.id.main_search_background);
        toolbarShadow = quickReturnView.findViewById(R.id.main_toolbar_shadow);
        toolbarGradient = quickReturnView.findViewById(R.id.main_toolbar_gradient);
    }

    /**
     * Update the WebView based on whose scroll position the search bar will hide itself.
     * @param webView The WebView against which scrolling will be tracked.
     */
    public void setScrollView(ObservableWebView webView) {
        webview = webView;
        webview.addOnScrollChangeListener(this);
        webview.addOnDownMotionEventListener(this);
        webview.addOnUpOrCancelMotionEventListener(this);
    }

    /**
     * Whether to enable fading in/out of the search bar when near the top of the article.
     * @param enabled True to enable fading, false otherwise.
     */
    public void setFadeEnabled(boolean enabled) {
        fadeEnabled = enabled;
        update();
    }

    /**
     * Whether to temporarily disable fading of the search bar, even if fading is enabled otherwise.
     * May be used when displaying a temporary UI element that requires the search bar to be shown
     * fully, e.g. when the ToC is pulled out.
     * @param force True to temporarily disable fading, false otherwise.
     */
    public void setForceNoFade(boolean force) {
        forceNoFade = force;
        update();
    }

    /**
     * Force an update of the appearance of the search bar. Usually it is updated automatically
     * when the associated WebView is scrolled, but this function may be used to manually refresh
     * the appearance of the search bar, e.g. when the WebView is first shown.
     */
    public void update() {
        if (webview == null) {
            return;
        }
        onScrollChanged(webview.getScrollY(), webview.getScrollY());
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // enable fading/translucent search bar only in API 11+
            final int fadeHeight = 256;
            final float searchBoxFadeOffset = 0.3f;
            float alpha = 1f;
            if (fadeEnabled && !forceNoFade) {
                alpha = (float) scrollY / (fadeHeight * displayDensity);
            }
            toolbarBackground.setAlpha(alpha);
            toolbarShadow.setAlpha(alpha);
            toolbarSearchBackground.setAlpha(Math.min(1f, alpha + searchBoxFadeOffset));
            toolbarGradient.setAlpha(1f - alpha);
        }
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
