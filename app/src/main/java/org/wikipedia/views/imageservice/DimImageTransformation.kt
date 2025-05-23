package org.wikipedia.views.imageservice

import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

class DimImageTransformation : Transformation(), ImageTransformer {
    override val cacheKey = "DimImageTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val result = if (input.isMutable) input else input.copy(input.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
        if (WikipediaApp.instance.currentTheme.isDark && Prefs.dimDarkModeImages) {
            result.applyCanvas {
                drawPaint(Paint().apply { color = Color.argb(100, 0, 0, 0) })
            }
        }
        return result
    }
}
