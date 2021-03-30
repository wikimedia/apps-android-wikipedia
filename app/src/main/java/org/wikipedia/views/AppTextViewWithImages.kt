package org.wikipedia.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.updateBounds
import androidx.core.text.set
import androidx.core.text.toSpannable
import kotlin.math.roundToInt

// Credit: https://stackoverflow.com/a/38977396
class AppTextViewWithImages constructor(context: Context, attrs: AttributeSet? = null) : AppTextView(context, attrs) {
    /**
     * A method to set a Spanned character sequence containing drawable resources.
     *
     * @param text A CharSequence formatted for use in android.text.TextUtils.expandTemplate(),
     * e.g.: "^1 is my favorite mobile operating system."  Placeholders are expected in
     * the format "^1", "^2", and so on.
     * @param drawableIds Numeric drawable IDs for the drawables which are to replace the
     * placeholders, in the order in which they should appear.
     */
    fun setTextWithDrawables(text: CharSequence, @DrawableRes vararg drawableIds: Int) {
        setText(text, getImageSpans(*drawableIds))
    }

    private fun getImageSpans(@DrawableRes vararg drawableIds: Int): List<Spanned> {
        return drawableIds.map { makeImageSpan(it, textSize, currentTextColor) }
    }

    private fun setText(text: CharSequence, spans: List<Spanned>) {
        if (spans.isNotEmpty()) {
            val spanned = TextUtils.expandTemplate(text, *spans.toTypedArray<CharSequence>())
            super.setText(spanned, BufferType.SPANNABLE)
        } else {
            super.setText(text)
        }
    }

    /**
     * Create an ImageSpan containing a drawable to be inserted in a TextView. This also sets the
     * image size and color.
     *
     * @param drawableId    A drawable resource Id.
     * @param size          The desired size (i.e. width and height) of the image icon in pixels.
     * @param color         The color to apply to the image.
     * @return A single-length ImageSpan that can be swapped into a CharSequence to replace a
     * placeholder.
     */
    @VisibleForTesting
    fun makeImageSpan(@DrawableRes drawableId: Int, size: Float, @ColorInt color: Int): Spannable {
        val result = " ".toSpannable()
        val drawable = getFormattedDrawable(drawableId, size, color)
        result[0, 1] = BaselineAlignedYTranslationImageSpan(drawable, lineSpacingMultiplier)
        return result
    }

    @VisibleForTesting
    fun getFormattedDrawable(@DrawableRes drawableId: Int, size: Float, @ColorInt color: Int): Drawable {
        val drawable = AppCompatResources.getDrawable(context, drawableId)!!
        drawable.setTint(color)
        val ratio = drawable.intrinsicWidth / drawable.intrinsicHeight.toFloat()
        drawable.updateBounds(right = size.roundToInt(), bottom = (size * ratio).roundToInt())
        return drawable
    }

    /**
     * An ImageSpan subclass that manually adjusts the vertical position of the drawable it contains
     * to correct for the failure of ImageSpan.ALIGN_BASELINE to take into account any adjustments
     * to the parent view's line height (via lineSpacingMultiplier or lineSpacingExtra).
     *
     * The general approach is adapted (and simplified) from http://stackoverflow.com/a/28361364.
     *
     * Not written as generically as it could be since I don't think there's any need for this kind
     * of tweak elsewhere at present, but could probably be made more generic (i.e., made not to
     * assume ALIGN_BASELINE and to also account for any lineSpacingExtra) and broken out into a
     * standalone class if need be.
     *
     * A possibly related issue is https://code.google.com/p/android/issues/detail?id=21397,
     * but note that the problem this works around affects an ImageSpan on any line, not just the
     * last line as reported there.
     */
    private class BaselineAlignedYTranslationImageSpan constructor(drawable: Drawable, private val lineSpacingMultiplier: Float) : ImageSpan(drawable, ALIGN_BASELINE) {
        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int,
                          y: Int, bottom: Int, paint: Paint) {
            val drawable = drawable
            canvas.save()
            var transY = bottom - drawable.bounds.bottom
            transY -= paint.fontMetricsInt.descent * lineSpacingMultiplier.toInt()
            canvas.translate(x, transY.toFloat())
            drawable.draw(canvas)
            canvas.restore()
        }
    }
}
