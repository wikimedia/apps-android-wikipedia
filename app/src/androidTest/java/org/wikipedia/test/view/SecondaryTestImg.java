package org.wikipedia.test.view;

import android.support.annotation.DrawableRes;

import org.wikipedia.R;

public enum SecondaryTestImg implements TestImg {
    NULL(0), CHECKERBOARD(R.drawable.checkerboard);

    @Override @DrawableRes public int id() {
        return id;
    }

    @Override public boolean isNull() {
        return this == NULL;
    }

    @Override public String toString() {
        return super.toString().toLowerCase();
    }

    @DrawableRes private final int id;
    SecondaryTestImg(@DrawableRes int id) {
        this.id = id;
    }
}
