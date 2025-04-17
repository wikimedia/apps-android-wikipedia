package org.wikipedia.views.imageservice

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.media.FaceDetector
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.Bitmap
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.transform.Transformation
import org.wikipedia.extensions.applyMatrixWithBackground
import org.wikipedia.extensions.maybeDimImage
import org.wikipedia.util.log.L

class CoilCenterCropWithFaceTransformation : Transformation() {
    override val cacheKey: String
        get() = "CoilCenterCropWithFaceTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = size.width.pxOrElse { 0 }
        val height = size.height.pxOrElse { 0 }

        if (input.width == width && input.height == height) {
            return input
        }

        var facePos: PointF? = null
        if (isBitmapEligibleForImageProcessing(input)) {
            val testBmp = new565ScaledBitmap(input)
            try {
                facePos = detectFace(testBmp)
            } catch (e: OutOfMemoryError) {
                L.logRemoteErrorIfProd(e)
            }
        }
        val scale: Float
        val dx: Float
        var dy: Float
        val half = 0.5f
        val m = Matrix()
        if (input.width * height > width * input.height) {
            scale = height.toFloat() / input.height.toFloat()
            dx = (width - input.width * scale) * half
            dy = 0f
        } else {
            scale = width.toFloat() / input.width.toFloat()
            dx = 0f
            dy = 0f
        }

        // apply face offset if we have one
        if (facePos != null) {
            dy = height * half - input.height * scale * facePos.y
            if (dy > 0) {
                dy = 0f
            } else if (dy < -(input.height * scale - height)) {
                dy = -(input.height * scale - height)
            }
        }
        m.setScale(scale, scale)
        m.postTranslate(dx, dy)
        val result = createBitmap(width, height,
            input.config ?: android.graphics.Bitmap.Config.RGB_565)
        // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
        result.setHasAlpha(input.hasAlpha())
        result.applyMatrixWithBackground(input, m)
        return result.maybeDimImage()
    }

    private fun detectFace(testBitmap: android.graphics.Bitmap): PointF? {
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

    private fun new565ScaledBitmap(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val copy = createBitmap(BITMAP_COPY_WIDTH, src.height * BITMAP_COPY_WIDTH / src.width, android.graphics.Bitmap.Config.RGB_565)
        return copy.applyCanvas {
            val srcRect = Rect(0, 0, src.width, src.height)
            val destRect = Rect(0, 0, BITMAP_COPY_WIDTH, copy.height)
            val paint = Paint()
            paint.color = Color.BLACK
            drawBitmap(src, srcRect, destRect, paint)
        }
    }

    private fun isBitmapEligibleForImageProcessing(bitmap: android.graphics.Bitmap): Boolean {
        val minSize = 64
        return bitmap.width >= minSize && bitmap.height >= minSize
    }

    companion object {
        private const val ID = "org.wikipedia.views.CenterCropWithFace"
        private const val BITMAP_COPY_WIDTH = 200
    }
}
