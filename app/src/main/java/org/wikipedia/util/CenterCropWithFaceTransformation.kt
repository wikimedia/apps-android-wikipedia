package org.wikipedia.util

import android.graphics.*
import android.media.FaceDetector
import androidx.core.graphics.applyCanvas
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import org.wikipedia.util.log.L
import java.security.MessageDigest

class CenterCropWithFaceTransformation : BitmapTransformation() {
    private val idBytes = ID.toByteArray(CHARSET)

    override fun equals(other: Any?): Boolean {
        return other is CenterCropWithFaceTransformation
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(idBytes)
    }

    override fun transform(pool: BitmapPool, inBitmap: Bitmap, width: Int, height: Int): Bitmap {
        if (inBitmap.width == width && inBitmap.height == height) {
            return inBitmap
        }
        var facePos: PointF? = null
        if (isBitmapEligibleForImageProcessing(inBitmap)) {
            val testBmp = new565ScaledBitmap(pool, inBitmap)
            try {
                facePos = detectFace(testBmp)
            } catch (e: OutOfMemoryError) {
                L.logRemoteErrorIfProd(e)
            }
            pool.put(testBmp)
        }
        val scale: Float
        val dx: Float
        var dy: Float
        val half = 0.5f
        val m = Matrix()
        if (inBitmap.width * height > width * inBitmap.height) {
            scale = height.toFloat() / inBitmap.height.toFloat()
            dx = (width - inBitmap.width * scale) * half
            dy = 0f
        } else {
            scale = width.toFloat() / inBitmap.width.toFloat()
            dx = 0f
            dy = 0f

            // apply face offset if we have one
            if (facePos != null) {
                dy = height * half - inBitmap.height * scale * facePos.y
                if (dy > 0) {
                    dy = 0f
                } else if (dy < -(inBitmap.height * scale - height)) {
                    dy = -(inBitmap.height * scale - height)
                }
            }
        }
        m.setScale(scale, scale)
        m.postTranslate(dx + half, dy + half)
        val result = pool.getDirty(width, height, getNonNullConfig(inBitmap))
        // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
        TransformationUtils.setAlpha(inBitmap, result)
        WhiteBackgroundTransformation().applyMatrixWithBackground(inBitmap, result, m)
        return result
    }

    private fun getNonNullConfig(bitmap: Bitmap): Bitmap.Config {
        return if (bitmap.config != null) bitmap.config else Bitmap.Config.RGB_565
    }

    private fun detectFace(testBitmap: Bitmap): PointF? {
        val maxFaces = 1
        val millis = System.currentTimeMillis()
        // initialize the face detector, and look for only one face...
        val fd = FaceDetector(testBitmap.width, testBitmap.height, maxFaces)
        val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)
        val numFound = fd.findFaces(testBitmap, faces)
        var facePos: PointF? = null
        if (numFound > 0) {
            facePos = PointF()
            faces[0]!!.getMidPoint(facePos)
            // center on the nose, not on the eyes
            facePos.y += faces[0]!!.eyesDistance() / 2
            // normalize the position to [0, 1]
            facePos[(facePos.x / testBitmap.width).coerceIn(0f, 1f)] = (facePos.y / testBitmap.height).coerceIn(0f, 1f)
            L.d("Found face at " + facePos.x + ", " + facePos.y)
        }
        L.d("Face detection took " + (System.currentTimeMillis() - millis) + "ms")
        return facePos
    }

    private fun new565ScaledBitmap(pool: BitmapPool, src: Bitmap): Bitmap {
        val copy = pool.getDirty(BITMAP_COPY_WIDTH, src.height * BITMAP_COPY_WIDTH / src.width, Bitmap.Config.RGB_565)
        return copy.applyCanvas {
            val srcRect = Rect(0, 0, src.width, src.height)
            val destRect = Rect(0, 0, BITMAP_COPY_WIDTH, copy.height)
            val paint = Paint()
            paint.color = Color.BLACK
            drawBitmap(src, srcRect, destRect, paint)
        }
    }

    private fun isBitmapEligibleForImageProcessing(bitmap: Bitmap): Boolean {
        val minSize = 64
        return bitmap.width >= minSize && bitmap.height >= minSize
    }

    companion object {
        private const val ID = "org.wikipedia.views.CenterCropWithFace"
        private const val BITMAP_COPY_WIDTH = 200
    }
}
