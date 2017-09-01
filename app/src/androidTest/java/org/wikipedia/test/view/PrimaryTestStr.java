package org.wikipedia.test.view;

import android.support.annotation.StringRes;

import org.wikipedia.R;

import java.util.Locale;

public enum PrimaryTestStr implements TestStr {
    NULL(0), SHORT(R.string.reading_list_name_sample),
    LONG(R.string.gallery_save_image_write_permission_rationale);

    @Override @StringRes public int id() {
        return id;
    }

    @Override public boolean isNull() {
        return this == NULL;
    }

    @Override public String toString() {
        return super.toString().toLowerCase(Locale.getDefault());
    }

    @StringRes private final int id;
    PrimaryTestStr(@StringRes int id) {
        this.id = id;
    }
}
