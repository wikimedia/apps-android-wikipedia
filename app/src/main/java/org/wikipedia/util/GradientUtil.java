package org.wikipedia.util;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;

import org.wikipedia.WikipediaApp;

public final class GradientUtil {
    private static final int GRADIENT_NUM_STOPS = 8;
    private static final int GRADIENT_POWER = 3;

    public static Drawable getPowerGradient(@ColorRes int baseColor, int gravity) {
        PaintDrawable drawable = new PaintDrawable();
        drawable.setShape(new RectShape());
        setPowerGradient(drawable, ContextCompat.getColor(WikipediaApp.getInstance(), baseColor), gravity);
        return drawable;
    }

    /**
     * Create a power gradient by using a compound gradient composed of a series of linear
     * gradients with intermediate color values.
     * adapted from: https://github.com/romannurik/muzei/blob/master/main/src/main/java/com/google/android/apps/muzei/util/ScrimUtil.java
     * @param baseColor The color from which the gradient starts (the ending color is transparent).
     * @param gravity Where the gradient should start from. Note: when making horizontal gradients,
     *                remember to use START/END, instead of LEFT/RIGHT.
     */
    private static void setPowerGradient(@NonNull PaintDrawable drawable, @ColorInt int baseColor, int gravity) {
        final int[] stopColors = new int[GRADIENT_NUM_STOPS];

        int red = Color.red(baseColor);
        int green = Color.green(baseColor);
        int blue = Color.blue(baseColor);
        int alpha = Color.alpha(baseColor);

        for (int i = 0; i < GRADIENT_NUM_STOPS; i++) {
            float x = i * 1f / (GRADIENT_NUM_STOPS - 1);
            float opacity = MathUtil.constrain((float) Math.pow(x, GRADIENT_POWER), 0.0f, 1.0f);
            stopColors[i] = Color.argb((int) (alpha * opacity), red, green, blue);
        }

        final float x0, x1, y0, y1;
        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.START:
                x0 = 1;
                x1 = 0;
                break;
            case Gravity.END:
                x0 = 0;
                x1 = 1;
                break;
            default:
                x0 = 0;
                x1 = 0;
                break;
        }
        switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                y0 = 1;
                y1 = 0;
                break;
            case Gravity.BOTTOM:
                y0 = 0;
                y1 = 1;
                break;
            default:
                y0 = 0;
                y1 = 0;
                break;
        }

        drawable.setShaderFactory(new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(width * x0, height * y0, width * x1, height * y1,
                                          stopColors, null, Shader.TileMode.CLAMP);
            }
        });
    }

    private GradientUtil() {
    }
}
