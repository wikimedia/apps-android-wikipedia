package org.wikipedia.util;

import com.nineoldandroids.animation.ArgbEvaluator;

public class ArgbEvaluatorCompat extends ArgbEvaluator {
    private static final int A_SHIFT = 24;
    private static final int R_SHIFT = 16;
    private static final int G_SHIFT = 8;
    private static final int UINT8_MASK = 0xff;

    @Override
    // Fix arithmetic shift of alpha. TODO: remove when nineoldandroids updated. See
    // * https://android.googlesource.com/platform/frameworks/base/+/9b55998
    // * https://github.com/JakeWharton/NineOldAndroids/commit/c91cb488f56b73aa81546a9fd5039ee99167be54
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> A_SHIFT) & UINT8_MASK;
        int startR = (startInt >> R_SHIFT) & UINT8_MASK;
        int startG = (startInt >> G_SHIFT) & UINT8_MASK;
        int startB = startInt & UINT8_MASK;

        int endInt = (Integer) endValue;
        int endA = (endInt >> A_SHIFT) & UINT8_MASK;
        int endR = (endInt >> R_SHIFT) & UINT8_MASK;
        int endG = (endInt >> G_SHIFT) & UINT8_MASK;
        int endB = endInt & UINT8_MASK;

        return ((startA + (int)(fraction * (endA - startA))) << A_SHIFT)
                | ((startR + (int)(fraction * (endR - startR))) << R_SHIFT)
                | ((startG + (int)(fraction * (endG - startG))) << G_SHIFT)
                | ((startB + (int)(fraction * (endB - startB))));
    }
}