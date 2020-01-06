package org.wikipedia.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.facebook.samples.zoomable.DefaultZoomableController
import com.facebook.samples.zoomable.ZoomableDraweeView

class ZoomableDraweeViewWithBackground @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : ZoomableDraweeView(context, attrs, defStyle) {
    private val backgroundPaint = Paint()
    private var drawBackground = false

    init {
        backgroundPaint.color = Color.WHITE
    }

    fun setDrawBackground(draw: Boolean) {
        drawBackground = draw
    }

    override fun onDraw(canvas: Canvas) {
        if (drawBackground) {
            val controller = zoomableController as DefaultZoomableController
            val saveCount = canvas.save()
            canvas.concat(controller.transform)
            canvas.drawRect(controller.imageBounds, backgroundPaint)
            canvas.restoreToCount(saveCount)
        }
        super.onDraw(canvas)
    }
}
