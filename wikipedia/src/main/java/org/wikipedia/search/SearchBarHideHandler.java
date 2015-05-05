package org.wikipedia.search;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.view.ViewHelper;

import org.wikipedia.R;
import org.wikipedia.ViewAnimations;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.views.ObservableWebView;

public class SearchBarHideHandler implements ObservableWebView.OnScrollChangeListener, ObservableWebView.OnUpOrCancelMotionEventListener, ObservableWebView.OnDownMotionEventListener {
    private static final int HUMAN_SCROLL_THRESHOLD = 200;
    private static final int FULL_OPACITY = 255;
    private final Activity parentActivity;
    private final View quickReturnView;
    private final float displayDensity;
    private final int toolbarColor;
    private final ArgbEvaluator colorEvaluator;

    private ObservableWebView webview;
    private boolean fadeEnabled = false;
    private boolean forceNoFade = false;
    private View toolbarBackground;
    private View toolbarGradient;
    private View toolbarShadow;

    public SearchBarHideHandler(Activity activity, View quickReturnView) {
        this.parentActivity = activity;
        this.quickReturnView =  quickReturnView;
        this.displayDensity = quickReturnView.getResources().getDisplayMetrics().density;

        toolbarBackground = quickReturnView.findViewById(R.id.main_toolbar);
        toolbarShadow = quickReturnView.findViewById(R.id.main_toolbar_shadow);
        toolbarGradient = quickReturnView.findViewById(R.id.main_toolbar_gradient);

        colorEvaluator = new ArgbEvaluator();
        toolbarColor = activity.getResources().getColor(R.color.actionbar_background);
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
        final int fadeHeight = 256;
        int opacity = FULL_OPACITY;
        if (fadeEnabled && !forceNoFade) {
            opacity = scrollY * FULL_OPACITY / (int) (fadeHeight * displayDensity);
        }
        opacity = Math.max(0, opacity);
        opacity = Math.min(FULL_OPACITY, opacity);
        toolbarBackground.getBackground().setAlpha(opacity);
        toolbarShadow.getBackground().setAlpha(opacity);
        toolbarGradient.getBackground().setAlpha(FULL_OPACITY - opacity);
        if (ApiUtil.hasLollipop()) {
            parentActivity.getWindow().setStatusBarColor(
                    (int) colorEvaluator
                            .evaluate((float) opacity / FULL_OPACITY, Color.BLACK, toolbarColor));
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
            if (scrollDelta > (int) (HUMAN_SCROLL_THRESHOLD * displayDensity)) {
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
