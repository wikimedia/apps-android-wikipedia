package org.wikipedia.page;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;

public class PageToolbarHideHandler extends ViewHideHandler {
    private static final int FULL_OPACITY = 255;

    @NonNull private final Context context;
    private boolean fadeEnabled;
    private boolean forceNoFade;
    @NonNull private final Drawable toolbarBackground;
    private Drawable toolbarGradient;
    @NonNull private final Drawable statusBar;

    public PageToolbarHideHandler(@NonNull Context context, @NonNull View hideableView) {
        super(hideableView, Gravity.TOP);
        this.context = context;

        LayerDrawable toolbarBackgroundLayers = (LayerDrawable) hideableView.getBackground();
        toolbarBackground = toolbarBackgroundLayers.findDrawableByLayerId(R.id.toolbar_background_solid).mutate();
        initToolbarGradient(toolbarBackgroundLayers);

        statusBar = hideableView.findViewById(R.id.empty_status_bar).getBackground().mutate();
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

    @Override
    protected void onScrolled(int oldScrollY, int scrollY) {
        int opacity = calculateScrollOpacity(scrollY);
        toolbarBackground.setAlpha(opacity);
        toolbarGradient.setAlpha(FULL_OPACITY - opacity);
        statusBar.setAlpha(opacity);
    }

    private void initToolbarGradient(LayerDrawable toolbarBackgroundLayers) {
        @ColorInt int baseColor = getColor(R.color.lead_gradient_start);
        toolbarGradient = GradientUtil.getCubicGradient(baseColor, Gravity.TOP);
        toolbarBackgroundLayers.setDrawableByLayerId(R.id.toolbar_background_gradient, toolbarGradient);
    }

    /** @return Alpha value between 0 and 0xff. */
    private int calculateScrollOpacity(int scrollY) {
        final int fadeHeight = 200;
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
