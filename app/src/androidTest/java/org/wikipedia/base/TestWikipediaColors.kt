package org.wikipedia.base

import androidx.annotation.ColorRes
import org.wikipedia.R
import org.wikipedia.theme.Theme

/**
 * Each type represents a specific use case for colors across different themes
 */
enum class TestThemeColorType {
    PAPER,
    BACKGROUND,
    BORDER,
    INACTIVE,
    PLACEHOLDER,
    SECONDARY,
    PRIMARY,
    PROGRESSIVE,
    SUCCESS,
    DESTRUCTIVE,
    WARNING,
    HIGHLIGHT,
    FOCUS,
    ADDITION,
    OVERLAY
}

object TestWikipediaColors {

    @ColorRes
    fun getGetColor(theme: Theme, colorType: TestThemeColorType): Int {
        return when (theme) {
            Theme.LIGHT -> getLightThemeColor(colorType)
            Theme.DARK -> getDarkThemeColor(colorType)
            Theme.BLACK -> getBlackThemeColor(colorType)
            Theme.SEPIA -> getSepiaThemeColor(colorType)
        }
    }

    @ColorRes
    private fun getLightThemeColor(colorType: TestThemeColorType): Int = when (colorType) {
        TestThemeColorType.PAPER -> R.color.white
        TestThemeColorType.BACKGROUND -> R.color.gray100
        TestThemeColorType.BORDER -> R.color.gray200
        TestThemeColorType.INACTIVE -> R.color.gray400
        TestThemeColorType.PLACEHOLDER -> R.color.gray500
        TestThemeColorType.SECONDARY -> R.color.gray600
        TestThemeColorType.PRIMARY -> R.color.gray700
        TestThemeColorType.PROGRESSIVE -> R.color.blue600
        TestThemeColorType.SUCCESS -> R.color.green700
        TestThemeColorType.DESTRUCTIVE -> R.color.red700
        TestThemeColorType.WARNING -> R.color.yellow700
        TestThemeColorType.HIGHLIGHT -> R.color.yellow500
        TestThemeColorType.FOCUS -> R.color.orange500
        TestThemeColorType.ADDITION -> R.color.blue300_15
        TestThemeColorType.OVERLAY -> R.color.black_30
    }

    @ColorRes
    private fun getDarkThemeColor(colorType: TestThemeColorType): Int = when (colorType) {
        TestThemeColorType.PAPER -> R.color.gray700
        TestThemeColorType.BACKGROUND -> R.color.gray675
        TestThemeColorType.BORDER -> R.color.gray650
        TestThemeColorType.INACTIVE -> R.color.gray500
        TestThemeColorType.PLACEHOLDER -> R.color.gray400
        TestThemeColorType.SECONDARY -> R.color.gray300
        TestThemeColorType.PRIMARY -> R.color.gray200
        TestThemeColorType.PROGRESSIVE -> R.color.blue300
        TestThemeColorType.SUCCESS -> R.color.green600
        TestThemeColorType.DESTRUCTIVE -> R.color.red500
        TestThemeColorType.WARNING -> R.color.orange500
        TestThemeColorType.HIGHLIGHT -> R.color.yellow500_40
        TestThemeColorType.FOCUS -> R.color.orange500_50
        TestThemeColorType.ADDITION -> R.color.blue600_30
        TestThemeColorType.OVERLAY -> R.color.black_70
    }

    @ColorRes
    private fun getSepiaThemeColor(colorType: TestThemeColorType): Int = when (colorType) {
        TestThemeColorType.PAPER -> R.color.beige100
        TestThemeColorType.BACKGROUND -> R.color.beige300
        TestThemeColorType.BORDER -> R.color.beige400
        TestThemeColorType.INACTIVE -> R.color.taupe200
        TestThemeColorType.PLACEHOLDER -> R.color.taupe600
        TestThemeColorType.SECONDARY -> R.color.gray600
        TestThemeColorType.PRIMARY -> R.color.gray700
        TestThemeColorType.PROGRESSIVE -> R.color.blue600
        TestThemeColorType.SUCCESS -> R.color.green700
        TestThemeColorType.DESTRUCTIVE -> R.color.red700
        TestThemeColorType.WARNING -> R.color.yellow700
        TestThemeColorType.HIGHLIGHT -> R.color.yellow500
        TestThemeColorType.FOCUS -> R.color.orange500
        TestThemeColorType.ADDITION -> R.color.blue300_15
        TestThemeColorType.OVERLAY -> R.color.black_30
    }

    @ColorRes
    private fun getBlackThemeColor(colorType: TestThemeColorType): Int = when (colorType) {
        TestThemeColorType.PAPER -> R.color.black
        TestThemeColorType.BACKGROUND -> R.color.gray700
        TestThemeColorType.BORDER -> R.color.gray675
        TestThemeColorType.INACTIVE -> R.color.gray500
        TestThemeColorType.PLACEHOLDER -> R.color.gray400
        TestThemeColorType.SECONDARY -> R.color.gray300
        TestThemeColorType.PRIMARY -> R.color.gray200
        TestThemeColorType.PROGRESSIVE -> R.color.blue300
        TestThemeColorType.SUCCESS -> R.color.green600
        TestThemeColorType.DESTRUCTIVE -> R.color.red500
        TestThemeColorType.WARNING -> R.color.orange500
        TestThemeColorType.HIGHLIGHT -> R.color.yellow500_40
        TestThemeColorType.FOCUS -> R.color.orange500_50
        TestThemeColorType.ADDITION -> R.color.blue600_30
        TestThemeColorType.OVERLAY -> R.color.black_70
    }
}
