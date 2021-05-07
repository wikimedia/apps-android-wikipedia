package org.wikipedia.feed.view

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader.TileMode
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewGradientCircleNumberBinding
import org.wikipedia.util.ResourceUtil

class GradientCircleNumberView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewGradientCircleNumberBinding.inflate(LayoutInflater.from(context), this)

    private val gradientColor1 = ResourceUtil.getThemedColor(context, R.attr.colorAccent)
    private val gradientColor2 = ContextCompat.getColor(context, R.color.green50)

    private fun applyGradient() {
        val textShader = LinearGradient(0f, 0f, 0f, binding.numberView.textSize,
                intArrayOf(gradientColor2, gradientColor1, gradientColor2),
                floatArrayOf(0f, 0.5f, 1f), TileMode.CLAMP)
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(gradientColor1, gradientColor2))
        gradientDrawable.cornerRadius = 90f
        binding.numberView.paint.shader = textShader
        binding.baseNumberView.background = gradientDrawable
    }

    fun setNumber(number: Int) {
        binding.numberView.text = number.toString()
        applyGradient()
    }
}
