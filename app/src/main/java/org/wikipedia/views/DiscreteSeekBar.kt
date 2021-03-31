package org.wikipedia.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.withStyledAttributes
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
        if (tickDrawable != null) {
            val halfW = if (tickDrawable!!.intrinsicWidth >= 0) tickDrawable!!.intrinsicWidth / 2 else 1
            val halfH = if (tickDrawable!!.intrinsicHeight >= 0) tickDrawable!!.intrinsicHeight / 2 else 1
            tickDrawable!!.setBounds(-halfW, -halfH, halfW, halfH)
        }
        if (centerDrawable != null) {
            val halfW = if (centerDrawable!!.intrinsicWidth >= 0) centerDrawable!!.intrinsicWidth / 2 else 1
            val halfH = if (centerDrawable!!.intrinsicHeight >= 0) centerDrawable!!.intrinsicHeight / 2 else 1
            centerDrawable!!.setBounds(-halfW, -halfH, halfW, halfH)
        }
        val tickSpacing = (width - paddingLeft - paddingRight).toFloat() / (maxNumber - minNumber).toFloat()
        canvas.save()
        if (isRtl) {
            canvas.scale(-1f, 1f, (width / 2).toFloat(), (height / 2).toFloat())
        }
        canvas.translate(paddingLeft.toFloat(), (height / 2).toFloat())
        for (i in minNumber..maxNumber) {
            if (drawOther && tickDrawable != null && i > value) {
                tickDrawable!!.draw(canvas)
            }
            if (drawCenter && i == 0 && centerDrawable != null) {
                centerDrawable!!.draw(canvas)
            }
            canvas.translate(tickSpacing, 0.0f)
        }
        canvas.restore()
    }
}
