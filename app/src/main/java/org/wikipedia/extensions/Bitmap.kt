package org.wikipedia.extensions

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.applyCanvas

fun Bitmap.applyMatrixWithBackground(inBitmap: Bitmap, matrix: Matrix) {
    val defaultPaint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    val paintWhite = Paint().apply { color = Color.WHITE }
    applyCanvas {
        drawRect(
            0f, 0f,
            width.toFloat(),
            height.toFloat(),
            paintWhite
        )
        drawBitmap(inBitmap, matrix, defaultPaint)
    }
}
