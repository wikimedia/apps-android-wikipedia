package org.wikipedia.test.view;

public enum Visibility {
    HIDDEN, VISIBLE;

    public boolean visible() {
        return this == VISIBLE;
    }
}