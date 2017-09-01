package org.wikipedia.page;

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class PageToolbarHideHandler extends ViewHideHandler {
    private static final int FULL_OPACITY = 255;

    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private boolean fadeEnabled;
    private boolean forceNoFade;
    @NonNull private PageFragment pageFragment;
    @NonNull private Toolbar toolbar;
    @NonNull private Drawable toolbarBackground;
    @NonNull private ImageView tabsButton;

    @ColorInt private int themedIconColor;
    @ColorInt private int baseStatusBarColor;
    @ColorInt private int themedStatusBarColor;

    public PageToolbarHideHandler(@NonNull PageFragment pageFragment, @NonNull View hideableView,
                                  @NonNull Toolbar toolbar, @NonNull ImageView tabsButton) {
        super(hideableView, null, Gravity.TOP);
        this.pageFragment = pageFragment;
        this.toolbar = toolbar;
        this.toolbarBackground = hideableView.getBackground().mutate();
        this.tabsButton = tabsButton;

        themedIconColor = getThemedColor(toolbar.getContext(), R.attr.page_toolbar_icon_color);
        baseStatusBarColor = ContextCompat.getColor(pageFragment.getContext(), R.color.base12);
        themedStatusBarColor = getThemedColor(toolbar.getContext(), R.attr.page_status_bar_color);
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
        tabsButton.setImageDrawable(ContextCompat.getDrawable(pageFragment.getContext(),
                ResourceUtil.getTabListIcon(pageFragment.getTabCount())));

        int opacity = calculateScrollOpacity(scrollY);
        toolbarBackground.setAlpha(opacity);
        updateChildIconTint(toolbar, opacity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pageFragment.getActivity().getWindow()
                    .setStatusBarColor(calculateStatusBarTintForOpacity(opacity));
        }
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

    private void updateChildIconTint(@NonNull ViewGroup viewGroup, float opacity) {
        int iconColor = calculateIconTintForOpacity(opacity);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView instanceof ImageView) {
                Drawable icon = ((ImageView) childView).getDrawable();
                if (icon != null) {
                    icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                }
            } else if (childView instanceof ViewGroup) {
                updateChildIconTint((ViewGroup) childView, opacity);
            }
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        }
    }

    /** @return A @ColorInt value between R.attr.page_toolbar_icon_color) and Color.WHITE. */
    @ColorInt
    private int calculateIconTintForOpacity(float opacity) {
        return (Integer) argbEvaluator.evaluate(opacity / FULL_OPACITY, Color.WHITE,
                themedIconColor);
    }

    @ColorInt
    private int calculateStatusBarTintForOpacity(float opacity) {
        return (Integer) argbEvaluator.evaluate(opacity / FULL_OPACITY, baseStatusBarColor,
                themedStatusBarColor);
    }
}
