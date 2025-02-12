package org.wikipedia.homeworks

import io.github.kakaocup.kakao.switch.KSwitch
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.wikipedia.R

val textSettingsCategory = KTextView {
    withId(R.id.textSettingsCategory)
    withText(R.string.theme_category_reading)
}

val textSizePercent = KTextView {
    withId(R.id.text_size_percent)
    withText(R.string.text_size_percent)
}

val buttonDecreaseTextSize = KTextView {
    withId(R.id.buttonDecreaseTextSize)
    withText(R.string.text_size_decrease)
}

val textSizeSeekBar = KTextView {
    withId(R.id.text_size_seek_bar)
}

val buttonIncreaseTextSize = KTextView {
    withId(R.id.buttonIncreaseTextSize)
    withText(R.string.text_size_increase)
}

val buttonFontFamilySansSerif = KButton {
    withId(R.id.button_font_family_sans_serif)
    withText(R.string.font_family_sans_serif)
}

val buttonFontFamilySerif = KButton {
    withId(R.id.button_font_family_serif)
    withText(R.string.font_family_serif)
}

val themeChooserReadingFocusModeSwitch = KSwitch {
    withId(R.id.theme_chooser_reading_focus_mode_switch)
    withText(R.string.reading_focus_mode)
}

val buttonThemeLight = KButton {
    withId(R.id.button_theme_light)
}

val buttonThemeSepia = KButton {
    withId(R.id.button_theme_sepia)
}

val buttonThemeDark = KButton {
    withId(R.id.button_theme_dark)
}

val buttonThemeBlack = KButton {
    withId(R.id.button_theme_black)
}

val themeChooserMatchSystemThemeSwitch = KSwitch {
    withId(R.id.theme_chooser_match_system_theme_switch)
    withText(R.string.theme_chooser_dialog_match_system_theme_switch_label)
}

val themeChooserDarkModeDimImagesSwitch = KSwitch {
    withId(R.id.theme_chooser_dark_mode_dim_images_switch)
    withText(R.string.theme_chooser_dialog_image_dimming_switch_label)
}

