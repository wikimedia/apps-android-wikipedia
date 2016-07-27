package org.wikipedia.overhaul.navtab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;

public class NavTabView extends TextView {
    @Nullable private Drawable icon;

    public NavTabView(Context context) {
        super(context, null, R.attr.navTabViewStyle);
    }

    public NavTabView icon(@DrawableRes int id) {
        boolean wide = bool(R.bool.wide);
        icon = drawable(id);
        setCompoundDrawablesWithIntrinsicBounds(wide ? icon : null, wide ? null : icon, null, null);
        return this;
    }

    public NavTabView text(@StringRes int id) {
        setText(id);
        return this;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    // It doesn't appear practical to tint an XML Drawable that itself is referenced in another XML
    // Drawable.
    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (icon == null) {
            return;
        }

        int color = color(isSelected() ? R.color.blue_liberal : R.color.gray_highlight);
        DrawableCompat.setTint(icon, color);
    }

    private Drawable drawable(@DrawableRes int id) {
        return ContextCompat.getDrawable(getContext(), id);
    }

    private boolean bool(@BoolRes int id) {
        return getResources().getBoolean(id);
    }

    @ColorInt private int color(@ColorRes int id) {
        return ContextCompat.getColor(getContext(), id);
    }
}