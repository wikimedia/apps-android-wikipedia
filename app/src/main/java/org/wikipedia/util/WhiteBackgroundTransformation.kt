package org.wikipedia.util

import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import org.wikipedia.extensions.applyMatrixWithBackground
import org.wikipedia.extensions.maybeDimImage
import org.wikipedia.views.imageservice.ImageTransformer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

open class WhiteBackgroundTransformation : BitmapTransformation(), ImageTransformer {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val bitmap = if (toTransform.hasAlpha()) {
            val result = pool[toTransform.width, toTransform.height, if (toTransform.config != null) toTransform.config else Bitmap.Config.RGB_565]
            result.applyMatrixWithBackground(toTransform, Matrix())
            result
        } else {
            toTransform
        }
        return bitmap.maybeDimImage()
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

    companion object {
        private const val ID = "org.wikipedia.util.WhiteBackgroundTransformation"
        private val ID_BYTES = ID.toByteArray(StandardCharsets.UTF_8)
    }
}
