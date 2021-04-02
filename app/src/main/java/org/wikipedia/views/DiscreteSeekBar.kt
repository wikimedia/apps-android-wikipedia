package org.wikipedia.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withSave
import org.wikipedia.R

class DiscreteSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        AppCompatSeekBar(context, attrs) {
    private var minNumber = 0
    private var tickDrawable: Drawable? = null
    private var centerDrawable: Drawable? = null
    private var isRtl = false

    var value: Int
        get() = progress + minNumber
        set(value) {
            progress = value - minNumber
        }

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.DiscreteSeekBar) {
                minNumber = getInteger(R.styleable.DiscreteSeekBar_min, 0)
                max -= minNumber
                val id = getResourceId(R.styleable.DiscreteSeekBar_tickDrawable, 0)
                if (id != 0) {
                    tickDrawable = AppCompatResources.getDrawable(context, id)
                }
                val id2 = getResourceId(R.styleable.DiscreteSeekBar_centerDrawable, 0)
                if (id2 != 0) {
                    centerDrawable = AppCompatResources.getDrawable(context, id2)
                }
            }
        }
        isRtl = resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL

        // Set this to false to prevent the issue of canvas not drawing in init{}
        setWillNotDraw(false)
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        if (value >= 0) {
            drawTickMarks(canvas, drawCenter = true, drawOther = false)
            super.onDraw(canvas)
            drawTickMarks(canvas, drawCenter = false, drawOther = true)
        } else {
            super.onDraw(canvas)
            drawTickMarks(canvas, drawCenter = true, drawOther = true)
        }
    }

    private fun drawTickMarks(canvas: Canvas, drawCenter: Boolean, drawOther: Boolean) {
        val maxNumber = max + minNumber
        tickDrawable?.let {
            val halfW = if (it.intrinsicWidth >= 0) it.intrinsicWidth / 2 else 1
            val halfH = if (it.intrinsicHeight >= 0) it.intrinsicHeight / 2 else 1
            it.setBounds(-halfW, -halfH, halfW, halfH)
        }
        centerDrawable?.let {
            val halfW = if (it.intrinsicWidth >= 0) it.intrinsicWidth / 2 else 1
            val halfH = if (it.intrinsicHeight >= 0) it.intrinsicHeight / 2 else 1
            it.setBounds(-halfW, -halfH, halfW, halfH)
        }
        val tickSpacing = (width - paddingLeft - paddingRight).toFloat() / (maxNumber - minNumber).toFloat()
        canvas.withSave {
            if (isRtl) {
                scale(-1f, 1f, (width / 2).toFloat(), (height / 2).toFloat())
            }
            translate(paddingLeft.toFloat(), (height / 2).toFloat())
            for (i in minNumber..maxNumber) {
                if (drawOther && i > value) {
                    tickDrawable?.draw(this)
                }
                if (drawCenter && i == 0) {
                    centerDrawable?.draw(this)
                }
                translate(tickSpacing, 0.0f)
            }
        }
    }
}
