package org.wikipedia.test.view;

public enum FontScale {
    DEFAULT(1), LARGE(1.5f);

    public float multiplier() {
        return multiplier;
    }

    @Override public String toString() {
        return super.toString().toLowerCase();
    }

    private final float multiplier;

    FontScale(float multiplier) {
        this.multiplier = multiplier;
    }
}
