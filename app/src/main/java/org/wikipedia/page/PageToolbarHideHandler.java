package org.wikipedia.page;

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.TabCountsView;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class PageToolbarHideHandler extends ViewHideHandler {
    private static final int FULL_OPACITY = 255;

    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private boolean fadeEnabled;
    @NonNull private PageFragment pageFragment;
    @NonNull private Toolbar toolbar;
    @NonNull private Drawable toolbarBackground;

    @ColorInt private int themedIconColor;
    @ColorInt private int baseStatusBarColor;
    @ColorInt private int themedStatusBarColor;

    private int toolbarHeight;

    PageToolbarHideHandler(@NonNull PageFragment pageFragment, @NonNull View hideableView,
                                  @NonNull Toolbar toolbar, @NonNull TabCountsView tabsButton) {
        super(hideableView, null, Gravity.TOP);
        this.pageFragment = pageFragment;
        this.toolbar = toolbar;
        this.toolbarBackground = hideableView.getBackground().mutate();
        themedIconColor = getThemedColor(toolbar.getContext(), R.attr.page_toolbar_icon_color);
        baseStatusBarColor = getThemedColor(toolbar.getContext(), R.attr.page_expanded_status_bar_color);
        themedStatusBarColor = getThemedColor(toolbar.getContext(), R.attr.page_status_bar_color);
        toolbarHeight = DimenUtil.getToolbarHeightPx(pageFragment.requireContext());
        tabsButton.updateTabCount();
    }

    /**
     * Whether to enable fading in/out of the search bar when near the top of the article.
     * @param enabled True to enable fading, false otherwise.
     */
    void setFadeEnabled(boolean enabled) {
        fadeEnabled = enabled;
        update();
    }

    @Override
    protected void onScrolled(int oldScrollY, int scrollY) {
        int opacity = fadeEnabled && scrollY < (DimenUtil.leadImageHeightForDevice() - toolbarHeight) ? 0 : FULL_OPACITY;
        toolbarBackground.setAlpha(opacity);
        updateChildIconTint(toolbar, opacity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pageFragment.requireActivity().getWindow()
                    .setStatusBarColor(calculateStatusBarTintForOpacity(opacity));
        }
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
            } else if (childView instanceof TabCountsView) {
                ((TabCountsView) childView).setColor(iconColor);
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
