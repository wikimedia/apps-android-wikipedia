package org.wikipedia.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.wikipedia.R
import org.wikipedia.model.EnumCode

enum class Theme(val marshallingId: Int, val funnelName: String, @field:StyleRes @get:StyleRes
@param:StyleRes val resourceId: Int, @field:StringRes @get:StringRes @param:StringRes val nameId: Int) : EnumCode {

    LIGHT(0, "light", R.style.ThemeLight, R.string.color_theme_light),
    DARK(1, "dark", R.style.ThemeDark, R.string.color_theme_dark),
    BLACK(2, "black", R.style.ThemeBlack, R.string.color_theme_black),
    SEPIA(3, "sepia", R.style.ThemeSepia, R.string.color_theme_sepia);

    override fun code(): Int {
        return marshallingId
    }

    val isDefault: Boolean
        get() = this == fallback

    val isDark: Boolean
        get() = this == DARK || this == BLACK

    companion object {
        @JvmStatic
        val fallback: Theme
            get() = LIGHT

        @JvmStatic
        fun ofMarshallingId(id: Int): Theme? {
            return values().find { it.marshallingId == id }
        }
    }
}
