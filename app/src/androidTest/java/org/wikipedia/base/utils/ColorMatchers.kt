package org.wikipedia.base.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

object ColorMatchers {
    sealed class ColorTarget {
        data object TextColor : ColorTarget()
        data object BackgroundColor : ColorTarget()
        data object Tint : ColorTarget()
    }

    fun withTextColor(colorResOrAttr: Int, isAttr: Boolean = false): Matcher<View> = withColor(colorResOrAttr, isAttr, ColorTarget.TextColor)

    fun withBackgroundColor(colorResOrAttr: Int, isAttr: Boolean = false): Matcher<View> = withColor(colorResOrAttr, isAttr, ColorTarget.BackgroundColor)

    fun withTintColor(colorResOrAttr: Int, isAttr: Boolean = false): Matcher<View> = withColor(colorResOrAttr, isAttr, ColorTarget.Tint)

    private fun withColor(
        colorResOrAttr: Int,
        isAttr: Boolean = false,
        target: ColorTarget
    ): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("with color ${getColorTargetDescription(target)}: $colorResOrAttr")
            }

            override fun matchesSafely(view: View): Boolean {
                val expectedColor = resolveColor(view.context, colorResOrAttr, isAttr)
                return when (target) {
                    ColorTarget.BackgroundColor -> matchBackgroundColor(view, expectedColor)
                    ColorTarget.TextColor -> matchTextColor(view, expectedColor)
                    ColorTarget.Tint -> matchTintColor(view, expectedColor)
                }
            }
        }
    }

    private fun resolveColor(context: Context, colorResOrAttr: Int, isAttr: Boolean): Int {
        return if (isAttr) {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(colorResOrAttr, typedValue, true)
            ContextCompat.getColor(context, typedValue.resourceId)
        } else {
            ContextCompat.getColor(context, colorResOrAttr)
        }
    }

    private fun matchTextColor(view: View, expectedColor: Int): Boolean {
        return when (view) {
            is TextView -> view.currentTextColor == expectedColor
            else -> false
        }
    }

    private fun matchBackgroundColor(view: View, expectedColor: Int): Boolean {
        val background = view.background
        return when {
            background != null -> {
                val backgroundColor = TypedValue()
                view.context.theme.resolveAttribute(
                    android.R.attr.colorBackground,
                    backgroundColor,
                    true
                )
                backgroundColor.data == expectedColor
            }
            else -> false
        }
    }

    private fun matchTintColor(view: View, expectedColor: Int): Boolean {
        return when (view) {
            is ImageView -> {
                val tintList = ImageViewCompat.getImageTintList(view)
                tintList?.defaultColor == expectedColor
            }
            else -> false
        }
    }

    private fun getColorTargetDescription(target: ColorTarget): String {
        return when (target) {
            ColorTarget.BackgroundColor -> "text color"
            ColorTarget.TextColor -> "background color"
            ColorTarget.Tint -> "tint color"
        }
    }
}
