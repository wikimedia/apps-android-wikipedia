package org.wikipedia.views.imageservice

import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import org.wikipedia.extensions.applyMatrixWithBackground
import org.wikipedia.extensions.maybeDimImage

class WhiteBackgroundTransformation : Transformation(), ImageTransformer {
    override val cacheKey: String
        get() = "WhiteBackgroundTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val result = if (input.hasAlpha()) {
            val newBitmap = createBitmap(
                input.width,
                input.height,
                input.config ?: android.graphics.Bitmap.Config.ARGB_8888
            )
            newBitmap.applyMatrixWithBackground(input, Matrix())
            newBitmap
        } else input

        return result.maybeDimImage()
    }
}
