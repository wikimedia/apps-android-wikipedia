package org.wikipedia.search;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.ViewAnimations;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.views.ObservableWebView;

public class SearchBarHideHandler implements ObservableWebView.OnScrollChangeListener,
        ObservableWebView.OnUpOrCancelMotionEventListener,
        ObservableWebView.OnDownMotionEventListener {
    private static final int FULL_OPACITY = 255;
    @NonNull private final View quickReturnView;

    @NonNull private final Context context;
    @Nullable private ObservableWebView webview;
    private boolean fadeEnabled;
    private boolean forceNoFade;
    @NonNull private final Drawable toolbarBackground;
    private Drawable toolbarGradient;
    @NonNull private final Drawable toolbarShadow;
    @NonNull private final Drawable statusBar;

    public SearchBarHideHandler(@NonNull Activity activity, @NonNull View quickReturnView) {
        context = activity;
        this.quickReturnView = quickReturnView;

        LayerDrawable toolbarBackgroundLayers = (LayerDrawable) quickReturnView
                .findViewById(R.id.main_toolbar_background_container).getBackground();
        toolbarBackground = toolbarBackgroundLayers.findDrawableByLayerId(R.id.toolbar_background_solid).mutate();
        toolbarShadow = quickReturnView.findViewById(R.id.main_toolbar_shadow).getBackground().mutate();
        initToolbarGradient(toolbarBackgroundLayers);

        statusBar = quickReturnView.findViewById(R.id.empty_status_bar).getBackground().mutate();
    }

    /**
     * Update the WebView based on whose scroll position the search bar will hide itself.
     * @param webView The WebView against which scrolling will be tracked.
     */
    public void setScrollView(@Nullable ObservableWebView webView) {
        webview = webView;
        if (webview != null) {
            webview.addOnScrollChangeListener(this);
            webview.addOnDownMotionEventListener(this);
            webview.addOnUpOrCancelMotionEventListener(this);
        }
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
        onScrollChanged(webview.getScrollY(), webview.getScrollY(), false);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        if (webview == null) {
            return;
        }
        int opacity = calculateScrollOpacity(scrollY);
        toolbarBackground.setAlpha(opacity);
        toolbarShadow.setAlpha(opacity);
        toolbarGradient.setAlpha(FULL_OPACITY - opacity);
        statusBar.setAlpha(opacity);

        if (scrollY <= webview.getHeight()) {
            // For the first screenful, ensure it always exists.
            ViewAnimations.ensureTranslationY(quickReturnView, 0);
            return;
        }
        int animMargin;
        if (oldScrollY > scrollY) {
            int minMargin = 0;
            int scrollDelta = oldScrollY - scrollY;
            int newMargin = (int) quickReturnView.getTranslationY() + scrollDelta;
            animMargin = Math.min(minMargin, newMargin);
        } else {
            // scroll down!
            int scrollDelta = scrollY - oldScrollY;
            if (!isHumanScroll) {
                // we've been scrolled programmatically, probably to go to
                // a specific section, so keep the toolbar shown.
                animMargin = 0;
            } else {
                int minMargin = -quickReturnView.getHeight();
                int newMargin = (int) quickReturnView.getTranslationY() - scrollDelta;
                animMargin = Math.max(minMargin, newMargin);
            }
        }
        quickReturnView.setTranslationY(animMargin);
    }

    @Override
    public void onUpOrCancelMotionEvent() {
        int transY = (int) quickReturnView.getTranslationY();
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

    private void initToolbarGradient(LayerDrawable toolbarBackgroundLayers) {
        @ColorInt int baseColor = getColor(R.color.lead_gradient_start);
        toolbarGradient = GradientUtil.getCubicGradient(baseColor, Gravity.TOP);
        toolbarBackgroundLayers.setDrawableByLayerId(R.id.toolbar_background_gradient, toolbarGradient);
    }

    /** @return Alpha value between 0 and 0xff. */
    private int calculateScrollOpacity(int scrollY) {
        final int fadeHeight = 256;
        int opacity = FULL_OPACITY;
        if (fadeEnabled && !forceNoFade) {
            opacity = scrollY * FULL_OPACITY / (int) (fadeHeight * DimenUtil.getDensityScalar());
        }
        opacity = Math.max(0, opacity);
        opacity = Math.min(FULL_OPACITY, opacity);
        return opacity;
    }

    @ColorInt private int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    @NonNull private Resources getResources() {
        return context.getResources();
    }
}
