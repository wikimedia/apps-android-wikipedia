package org.wikipedia.drawable;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/** A {@link PorterDuffColorFilter} with accessors. */
public class ExposedPorterDuffColorFilter extends PorterDuffColorFilter {
    private final int color;
    private final PorterDuff.Mode mode;

    public ExposedPorterDuffColorFilter(int color, PorterDuff.Mode mode) {
        super(color, mode);
        this.color = color;
        this.mode = mode;
    }

    public int getColor() {
        return color;
    }

    public PorterDuff.Mode getMode() {
        return mode;
    }
}