package org.wikipedia.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import org.wikipedia.R;
import org.wikipedia.model.EnumCode;

public enum Theme implements EnumCode {
    LIGHT(0, "light", R.style.ThemeLight, R.string.color_theme_light, "pagelib_theme_light"),
    DARK(1, "dark", R.style.ThemeDark, R.string.color_theme_dark, "pagelib_theme_dark"),
    BLACK(2, "black", R.style.ThemeBlack, R.string.color_theme_black, "pagelib_theme_black"),
    SEPIA(3, "sepia", R.style.ThemeSepia, R.string.color_theme_sepia, "pagelib_theme_sepia");

    private final int marshallingId;
    private final String funnelName;
    @StyleRes private final int resourceId;
    @StringRes private final int nameId;
    private final String pageLibClass;

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

    public String getPageLibClass() {
        return pageLibClass;
    }

    public boolean isDefault() {
        return this == getFallback();
    }

    public boolean isDark() {
        return this == DARK || this == BLACK;
    }

    Theme(int marshallingId, String funnelName, @StyleRes int resourceId, @StringRes int nameId, String pageLibClass) {
        this.marshallingId = marshallingId;
        this.funnelName = funnelName;
        this.resourceId = resourceId;
        this.nameId = nameId;
        this.pageLibClass = pageLibClass;
    }
}
