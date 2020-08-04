package org.wikipedia.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import org.wikipedia.R;
import org.wikipedia.model.EnumCode;
import org.wikipedia.settings.Prefs;

import static org.wikipedia.Constants.DARK_MODES_TEXT_DARK;
import static org.wikipedia.Constants.DARK_MODES_TEXT_MEDIUM;

public enum Theme implements EnumCode {
    LIGHT(0, "light", R.style.ThemeLight, R.string.color_theme_light),
    DARK(1, "dark", R.style.ThemeDark, R.string.color_theme_dark),
    BLACK(2, "black", R.style.ThemeBlack, R.string.color_theme_black),
    SEPIA(3, "sepia", R.style.ThemeSepia, R.string.color_theme_sepia);

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

    @SuppressWarnings("magicnumber")
    @StyleRes public int getResourceId() {
        if (marshallingId == 0 || marshallingId == 3) {
            return resourceId;
        }
        switch (Prefs.getDarkModesTextColorLevelSelection()) {
            case DARK_MODES_TEXT_MEDIUM:
                return R.style.ThemeDarkTextMedium;
            case DARK_MODES_TEXT_DARK:
                return R.style.ThemeDarkTextDark;
            default:
                return (marshallingId == 1) ? R.style.ThemeDark : R.style.ThemeBlack;
        }
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
