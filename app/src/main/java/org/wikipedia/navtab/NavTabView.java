package org.wikipedia.navtab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;
import android.widget.TextView;

import org.wikipedia.R;

public class NavTabView extends TextView {
    @Nullable private Drawable icon;

    public NavTabView(Context context) {
        super(context, null, R.attr.navTabViewStyle);
    }

    public NavTabView icon(@DrawableRes int id) {
        icon = drawable(id);
        setCompoundDrawablesRelativeWithIntrinsicBounds(null, icon, null, null);
        return this;
    }

    public NavTabView text(@StringRes int id) {
        setText(id);
        return this;
    }

    @Override public void setCompoundDrawablesRelativeWithIntrinsicBounds(@Nullable Drawable start,
                                                                          @Nullable Drawable top,
                                                                          @Nullable Drawable end,
                                                                          @Nullable Drawable bottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            super.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    // It doesn't appear practical to tint an XML Drawable that itself is referenced in another XML
    // Drawable.
    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (icon == null) {
            return;
        }

        int color = color(isSelected() ? R.color.foundation_blue : R.color.foundation_gray);
        DrawableCompat.setTint(icon, color);
        setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(isSelected()
                        ? R.dimen.nav_tab_active_text_size : R.dimen.nav_tab_inactive_text_size));
    }

    private Drawable drawable(@DrawableRes int id) {
        return ContextCompat.getDrawable(getContext(), id);
    }

    @ColorInt private int color(@ColorRes int id) {
        return ContextCompat.getColor(getContext(), id);
    }
}