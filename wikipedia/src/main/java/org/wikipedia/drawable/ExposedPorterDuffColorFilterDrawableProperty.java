package org.wikipedia.drawable;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.util.Property;

import org.wikipedia.util.ApiUtil;

/** This property supports {@link ExposedPorterDuffColorFilter} animations on {@link Drawable}s. */
public class ExposedPorterDuffColorFilterDrawableProperty extends Property<Drawable, Integer> {
    public static ObjectAnimator objectAnimator(String propertyName,
                                                @NonNull Drawable drawable,
                                                @ColorInt int fromColor,
                                                @ColorInt int toColor) {
        drawable.setColorFilter(newColorFilter(fromColor));

        //noinspection unchecked
        return ObjectAnimator.ofObject(drawable,
                new ExposedPorterDuffColorFilterDrawableProperty(propertyName),
                new ArgbEvaluator(),
                fromColor,
                toColor);
    }

    public static ExposedPorterDuffColorFilter newColorFilter(@ColorInt int color) {
        return new ExposedPorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public ExposedPorterDuffColorFilterDrawableProperty(String name) {
        super(Integer.class, name);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Integer get(Drawable drawable) {
        if (ApiUtil.hasLollipop()) {
            if (drawable.getColorFilter() instanceof ExposedPorterDuffColorFilter) {
                ExposedPorterDuffColorFilter filter = (ExposedPorterDuffColorFilter) drawable.getColorFilter();
                return filter.getColor();
            }
        }

        // Client did not initialize the Drawable with a ExposedPorterDuffColorFilter.
        return Color.TRANSPARENT;
    }

    @Override
    public void set(Drawable drawable, @ColorInt Integer value) {
        drawable.setColorFilter(buildColorFilter(value));
    }

    protected ExposedPorterDuffColorFilter buildColorFilter(@ColorInt int color) {
        return newColorFilter(color);
    }
}