package org.wikipedia.test.view;

import android.support.annotation.DrawableRes;

import org.wikipedia.R;

public enum PrimaryTestImg implements TestImg {
    NULL(0), NONNULL(R.drawable.ic_wmf_logo);

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
    PrimaryTestImg(@DrawableRes int id) {
        this.id = id;
    }
}
