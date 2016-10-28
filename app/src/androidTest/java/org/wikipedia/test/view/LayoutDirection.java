package org.wikipedia.test.view;

public enum LayoutDirection {
    LOCALE, RTL;

    @Override public String toString() {
        return super.toString().toLowerCase();
    }
}