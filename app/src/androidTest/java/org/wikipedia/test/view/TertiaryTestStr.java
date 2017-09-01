package org.wikipedia.test.view;

import android.support.annotation.StringRes;

import org.wikipedia.R;

import java.util.Locale;

public enum TertiaryTestStr implements TestStr {
    SHORT(R.string.description_edit_success_done), LONG(R.string.preference_summary_show_images);

    @Override @StringRes public int id() {
        return id;
    }

    @Override public boolean isNull() {
        return false;
    }

    @Override public String toString() {
        return super.toString().toLowerCase(Locale.getDefault());
    }

    @StringRes private final int id;
    TertiaryTestStr(@StringRes int id) {
        this.id = id;
    }
}
