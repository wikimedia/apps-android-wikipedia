package org.wikipedia.feed.view

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_linear_gradient_number.view.*
import org.wikipedia.R


internal class LinearGradientNumberView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_linear_gradient_number, this)
    }

    fun setNumber(number: Int) {
        baseNumberView.text = number.toString()
        innerNumberView.text = number.toString()
        applyTextShader()
    }

    private fun applyTextShader() {
        val textShader: Shader = LinearGradient(0f, 0f, 0f, baseNumberView.textSize,
                intArrayOf(ContextCompat.getColor(context, R.color.red50),
                        ContextCompat.getColor(context, R.color.accent50),
                        ContextCompat.getColor(context, R.color.green50),
                        ContextCompat.getColor(context, R.color.yellow50),
                        ContextCompat.getColor(context, R.color.red50)),
                floatArrayOf(0f, 0.33f, 0.6f, 0.83f, 1f), TileMode.CLAMP)
        baseNumberView.paint.shader = textShader
    }
}
