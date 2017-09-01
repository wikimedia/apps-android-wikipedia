package org.wikipedia.test.view;

import java.util.Locale;

public enum FontScale {
    DEFAULT(1), LARGE(1.5f);

    public float multiplier() {
        return multiplier;
    }

    @Override public String toString() {
        return super.toString().toLowerCase(Locale.getDefault());
    }

    private final float multiplier;

    FontScale(float multiplier) {
        this.multiplier = multiplier;
    }
}
