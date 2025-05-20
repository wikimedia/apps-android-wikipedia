package org.wikipedia.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs

fun Bitmap.maybeDimImage(): Bitmap {
    val bitmap = this
    if (WikipediaApp.instance.currentTheme.isDark && Prefs.dimDarkModeImages) {
        val paintDarkOverlay = Paint().apply { color = Color.argb(100, 0, 0, 0) }
        val newBitmap = createBitmap(
            bitmap.width,
            bitmap.height
        )
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paintDarkOverlay)
        return newBitmap
    }
    return bitmap
}

fun Bitmap.applyMatrixWithBackground(inBitmap: Bitmap, matrix: Matrix) {
    val targetBitmap = this
    val defaultPaint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    val paintWhite = Paint().apply { color = Color.WHITE }
    val canvas = Canvas(this)
    canvas.drawRect(
        0f, 0f,
        targetBitmap.width.toFloat(),
        targetBitmap.height.toFloat(),
        paintWhite
    )
    canvas.drawBitmap(inBitmap, matrix, defaultPaint)
}
