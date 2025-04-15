package org.wikipedia.views.imageservice

import android.content.Context
import android.graphics.Bitmap
import coil3.size.Size
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import org.wikipedia.util.WhiteBackgroundTransformation

class CoilTransformerAdapter : ImageTransformer {
    private val whiteBackgroundTransformation = CoilWhiteBackgroundTransformation()
     suspend fun transform(bitmap: Bitmap): Bitmap {
        return whiteBackgroundTransformation.transform(bitmap, Size(bitmap.width, bitmap.height))
    }
}

class AccessibleGlideTransformFunction : WhiteBackgroundTransformation() {
    fun publicTransform(pool: BitmapPool, bitmap: Bitmap): Bitmap {
        return transform(pool, bitmap, bitmap.width, bitmap.height)
    }
}

class GlideTransformerAdapter(private val context: Context) : ImageTransformer {

    private val whiteBackgroundTransformation = AccessibleGlideTransformFunction()

    fun transform(bitmap: Bitmap): Bitmap {
        val pool = Glide.get(context).bitmapPool
        return whiteBackgroundTransformation.publicTransform(pool, bitmap)
    }
}

object ImageTransformationFactory {
    enum class TransformationLibrary { COIL, GLIDE }

    fun createBackgroundTransformation(
        transformationLibrary: TransformationLibrary,
        context: Context? = null
    ): ImageTransformer {
        return when (transformationLibrary) {
            TransformationLibrary.COIL -> CoilTransformerAdapter()
            TransformationLibrary.GLIDE -> {
                requireNotNull(context) { "Context is required for Glide transformation" }
                GlideTransformerAdapter(context)
            }
        }
    }
}
