package org.wikipedia.views.imageservice

import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

class DimImageTransformation(
    private val dimImages: Boolean = WikipediaApp.instance.currentTheme.isDark && Prefs.dimDarkModeImages
) : Transformation(), ImageTransformer {
    // The dimming state must be part of the cache key, otherwise a bitmap that was already
    // transformed for one theme is reused for the other theme.
    override val cacheKey = "DimImageTransformation:$dimImages"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val result = if (input.isMutable) input else input.copy(input.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
        if (dimImages) {
            result.applyCanvas {
                drawPaint(Paint().apply { color = Color.argb(51, 0, 0, 0) })
            }
        }
        return result
    }
}
