package org.wikipedia.test.view;

import java.util.Locale;

public enum LayoutDirection {
    LOCALE, RTL;

    public boolean isRtl() {
        return this == RTL;
    }

    @Override public String toString() {
        return super.toString().toLowerCase(Locale.getDefault());
    }
}
