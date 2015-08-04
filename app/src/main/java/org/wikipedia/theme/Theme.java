package org.wikipedia.theme;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;

public enum Theme {
    LIGHT(0, "light", R.style.Theme_WikiLight),
    DARK(1, "dark", R.style.Theme_WikiDark);

    private final int marshallingId;
    private final String funnelName;
    private final int resourceId;

    public static Theme getFallback() {
        return LIGHT;
    }

    @Nullable
    public static Theme ofMarshallingId(int id) {
        for (Theme theme : values()) {
            if (theme.getMarshallingId() == id) {
                return theme;
            }
        }
        return null;
    }

    public int getMarshallingId() {
        return marshallingId;
    }

    @NonNull
    public String getFunnelName() {
        return funnelName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public boolean isLight() {
        return this == LIGHT;
    }

    public boolean isDark() {
        return !isLight();
    }

    Theme(int marshallingId, String funnelName, int resourceId) {
        this.marshallingId = marshallingId;
        this.funnelName = funnelName;
        this.resourceId = resourceId;
    }
}