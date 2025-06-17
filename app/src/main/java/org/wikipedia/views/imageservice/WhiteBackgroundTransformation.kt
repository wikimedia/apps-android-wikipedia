package org.wikipedia.views.imageservice

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.applyCanvas
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

class WhiteBackgroundTransformation : Transformation(), ImageTransformer {
    override val cacheKey = "WhiteBackgroundTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (input.hasAlpha()) {
            val result = if (input.isMutable) input else input.copy(input.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
            val paint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                color = android.graphics.Color.WHITE
            }
            result.applyCanvas {
                drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), paint)
            }
            return result
        } else {
            return input
        }
    }
}
