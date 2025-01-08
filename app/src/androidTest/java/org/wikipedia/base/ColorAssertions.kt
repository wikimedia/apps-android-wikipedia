package org.wikipedia.base

import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.test.espresso.ViewAssertion
import com.google.android.material.imageview.ShapeableImageView
import org.junit.Assert.assertTrue

object ColorAssertions {
    sealed class ColorType {
        data object TextColor : ColorType()
        data object BackgroundColor : ColorType()
        data object Tint : ColorType()
        data object ShapeableImageViewColor : ColorType()
    }

    fun hasColor(
        @ColorRes colorResId: Int,
        colorType: ColorType = ColorType.TextColor
    ) = ViewAssertion { view, _ ->
        val context = view.context
        val expectedColor = ContextCompat.getColor(context, colorResId)
        val actualColor = when (colorType) {
            ColorType.BackgroundColor -> (view.background as ColorDrawable).color
            ColorType.TextColor -> (view as TextView).currentTextColor
            ColorType.Tint -> ImageViewCompat.getImageTintList(view as ImageView)?.defaultColor
            ColorType.ShapeableImageViewColor -> {
                val targetView = (view as ShapeableImageView)
                val colorDrawable = targetView.drawable as? ColorDrawable
                colorDrawable?.color
            }
        }

        assertTrue(
            "expectedColor: $expectedColor, actualColor: $actualColor",
            expectedColor.toHexString() == actualColor?.toHexString()
        )
    }

    private fun Int.toHexString() = String.format("#%06x", this)
}
