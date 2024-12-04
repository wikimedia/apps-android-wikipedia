package org.wikipedia.base

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.test.espresso.ViewAssertion
import org.junit.Assert.assertTrue

object ColorAssertions {
    sealed class ColorType {
        data object TextColor : ColorType()
        data object BackgroundColor : ColorType()
        data object Tint : ColorType()
    }

    fun hasColor(
        colorResOrAttr: Int,
        isAttr: Boolean = false,
        colorType: ColorType
    ) = ViewAssertion { view, noViewFoundExecption ->
        val context = view.context
        val expectedColor = resolveColor(context, colorResOrAttr, isAttr)
        val actualColor = when (colorType) {
            ColorType.BackgroundColor -> (view.background as ColorDrawable).color
            ColorType.TextColor -> (view as TextView).currentTextColor
            ColorType.Tint -> ImageViewCompat.getImageTintList(view as ImageView)?.defaultColor
        }

        assertTrue(
            "expectedColor: $expectedColor, actualColor: $actualColor",
            expectedColor.toHexString() == actualColor?.toHexString()
        )
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

    private fun Int.toHexString() = String.format("#%06x", this)
}
