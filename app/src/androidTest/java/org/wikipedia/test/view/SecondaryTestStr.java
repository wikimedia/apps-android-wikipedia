package org.wikipedia.test.view;

import android.support.annotation.StringRes;

import org.wikipedia.R;

import java.util.Locale;

public enum SecondaryTestStr implements TestStr {
    NULL(0), SHORT(R.string.reading_list_untitled), LONG(R.string.reading_lists_empty_message);

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
    SecondaryTestStr(@StringRes int id) {
        this.id = id;
    }
}
