package org.wikipedia.theme;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;

import org.wikipedia.R;
import org.wikipedia.model.EnumCode;

public enum Theme implements EnumCode {
    LIGHT(0, "light", R.style.ThemeLight, R.string.color_theme_light),
    DARK(1, "dark", R.style.ThemeDark, R.string.color_theme_dark),
    BLACK(2, "black", R.style.ThemeBlack, R.string.color_theme_black);

    private final int marshallingId;
    private final String funnelName;
    @StyleRes private final int resourceId;
    @StringRes private final int nameId;

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

    @Override public int code() {
        return marshallingId;
    }

    @NonNull
    public String getFunnelName() {
        return funnelName;
    }

    @StyleRes public int getResourceId() {
        return resourceId;
    }

    @StringRes public int getNameId() {
        return nameId;
    }

    public boolean isDefault() {
        return this == getFallback();
    }

    public boolean isDark() {
        return this == DARK || this == BLACK;
    }

    Theme(int marshallingId, String funnelName, @StyleRes int resourceId, @StringRes int nameId) {
        this.marshallingId = marshallingId;
        this.funnelName = funnelName;
        this.resourceId = resourceId;
        this.nameId = nameId;
    }
}
