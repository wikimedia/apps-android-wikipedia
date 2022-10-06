package org.wikipedia.util

import android.graphics.*
import androidx.core.graphics.applyCanvas
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class WhiteBackgroundTransformation : BitmapTransformation() {

    init {
        val blackAlpha = 100
        PAINT_WHITE.color = Color.WHITE
        PAINT_DARK_OVERLAY.color = Color.argb(blackAlpha, 0, 0, 0)
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return if (toTransform.hasAlpha()) {
            val result = pool[toTransform.width, toTransform.height, if (toTransform.config != null) toTransform.config else Bitmap.Config.RGB_565]
            applyMatrixWithBackground(toTransform, result, Matrix())
            result
        } else {
            toTransform
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is WhiteBackgroundTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    fun applyMatrixWithBackground(inBitmap: Bitmap, targetBitmap: Bitmap, matrix: Matrix) {
        TransformationUtils.getBitmapDrawableLock().lock()
        try {
            targetBitmap.applyCanvas {
                drawRect(0f, 0f, targetBitmap.width.toFloat(), targetBitmap.height.toFloat(), PAINT_WHITE)
                drawBitmap(inBitmap, matrix, DEFAULT_PAINT)
                if (WikipediaApp.instance.currentTheme.isDark && Prefs.dimDarkModeImages) {
                    // "dim" images by drawing a translucent black rectangle over them.
                    drawRect(0f, 0f, targetBitmap.width.toFloat(), targetBitmap.height.toFloat(), PAINT_DARK_OVERLAY)
                }
            }
        } finally {
            TransformationUtils.getBitmapDrawableLock().unlock()
        }
    }

    companion object {
        private const val ID = "org.wikipedia.util.WhiteBackgroundTransformation"
        private const val PAINT_FLAGS = Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG
        private val ID_BYTES = ID.toByteArray(StandardCharsets.UTF_8)
        private val DEFAULT_PAINT = Paint(PAINT_FLAGS)
        private val PAINT_WHITE = Paint()
        private val PAINT_DARK_OVERLAY = Paint()
    }
}
