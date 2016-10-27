package org.wikipedia.test.view;

public enum NullValue {
    NULL, NONNULL;

    @Override public String toString() {
        return super.toString().toLowerCase();
    }

    public boolean isNull() {
        return this == NULL;
    }
}