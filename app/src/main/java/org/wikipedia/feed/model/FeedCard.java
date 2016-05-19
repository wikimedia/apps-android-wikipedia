package org.wikipedia.feed.model;

import android.support.annotation.ColorInt;

import java.util.Random;

public class FeedCard {
    // TODO: [Feed] remove this fun data and fill in model data.
    private final int height;
    @ColorInt private final int color;

    public FeedCard() {
        height = randomHeight();
        color = randomColor();
    }

    public int height() {
        return height;
    }

    @ColorInt public int color() {
        return color;
    }

    @ColorInt int randomColor() {
        final int alphaMask = 0xff000000;
        final int colorMask = 0x00ffffff;
        return new Random().nextInt(colorMask) | alphaMask;
    }

    private int randomHeight() {
        final int minHeight = 128;
        final int range = 384;
        return minHeight + new Random().nextInt(range);
    }

}