package org.wikipedia.yearinreview.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withTranslation
import org.wikipedia.R
import kotlin.math.roundToInt

object YearInReviewShareImage {
    private const val WORDMARK_WIDTH_RATIO = 0.3f
    private const val TEXT_SIZE_RATIO = 0.025f
    private const val VERTICAL_MARGIN_RATIO = 0.02f
    private const val HORIZONTAL_MARGIN_RATIO = 0.08f

    // Luminance at which black and white have equal contrast against the background
    private const val LIGHT_CONTENT_MAX_BACKGROUND_LUMINANCE = 0.179

    fun decorate(context: Context, screenshot: Bitmap): Bitmap {
        val bitmap = screenshot.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        drawWordmark(context, bitmap, canvas)
        drawCreatedWithText(context, bitmap, canvas)
        return bitmap
    }

    private fun drawWordmark(context: Context, bitmap: Bitmap, canvas: Canvas) {
        val wordmark = AppCompatResources.getDrawable(context, R.drawable.wp_wordmark)?.mutate() ?: return
        val width = (bitmap.width * WORDMARK_WIDTH_RATIO).roundToInt()
        val height = width * wordmark.intrinsicHeight / wordmark.intrinsicWidth
        val left = (bitmap.width - width) / 2
        val top = (bitmap.height * VERTICAL_MARGIN_RATIO).roundToInt()
        wordmark.bounds = Rect(left, top, left + width, top + height)
        wordmark.colorFilter = PorterDuffColorFilter(contentColorFor(bitmap, wordmark.bounds), PorterDuff.Mode.SRC_IN)
        wordmark.draw(canvas)
    }

    private fun drawCreatedWithText(context: Context, bitmap: Bitmap, canvas: Canvas) {
        val text = context.getString(R.string.yir_feature_card_created_with)
        val horizontalMargin = (bitmap.width * HORIZONTAL_MARGIN_RATIO).roundToInt()
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = bitmap.width * TEXT_SIZE_RATIO
            typeface = ResourcesCompat.getFont(context, R.font.robotoflex_regular)
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, bitmap.width - 2 * horizontalMargin)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        val top = bitmap.height - (bitmap.height * VERTICAL_MARGIN_RATIO).roundToInt() - layout.height
        paint.color = contentColorFor(bitmap, Rect(horizontalMargin, top, bitmap.width - horizontalMargin, top + layout.height))
        canvas.withTranslation(horizontalMargin.toFloat(), top.toFloat()) {
            layout.draw(this)
        }
    }

    private fun contentColorFor(bitmap: Bitmap, region: Rect): Int {
        val sampleRegion = Rect(region)
        if (!sampleRegion.intersect(0, 0, bitmap.width, bitmap.height)) {
            return Color.WHITE
        }
        val pixels = IntArray(sampleRegion.width() * sampleRegion.height())
        bitmap.getPixels(pixels, 0, sampleRegion.width(), sampleRegion.left, sampleRegion.top, sampleRegion.width(), sampleRegion.height())
        val averageLuminance = pixels.sumOf { ColorUtils.calculateLuminance(it) } / pixels.size
        return if (averageLuminance < LIGHT_CONTENT_MAX_BACKGROUND_LUMINANCE) Color.WHITE else Color.BLACK
    }
}
