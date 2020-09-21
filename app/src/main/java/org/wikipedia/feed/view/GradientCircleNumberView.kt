package org.wikipedia.feed.view

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_gradient_circle_number.view.*
import org.wikipedia.R


internal class GradientCircleNumberView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val gradientColor1 = ContextCompat.getColor(context, R.color.accent50)
    private val gradientColor2 = ContextCompat.getColor(context, R.color.green50)

    init {
        View.inflate(context, R.layout.view_gradient_circle_number, this)
    }

    fun setNumber(number: Int) {
        numberView.text = number.toString()
        applyGradient()
    }

    private fun applyGradient() {
        val textShader: Shader = LinearGradient(0f, 0f, 0f, numberView.textSize,
                intArrayOf(gradientColor2, gradientColor1, gradientColor2),
                floatArrayOf(0f, 0.5f, 1f), TileMode.CLAMP)
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(gradientColor1, gradientColor2))
        gradientDrawable.cornerRadius = 30f
        numberView.paint.shader = textShader
        baseNumberView.background = gradientDrawable
    }
}
