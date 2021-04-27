package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewSuggestedEditsDailyProgressBinding

class DailyProgressView : ConstraintLayout {

    var binding = ViewSuggestedEditsDailyProgressBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun setMaximum(max: Int) {
        binding.progressBar.max = max * 100
    }

    fun update(oldProgress: Int, newProgress: Int, max: Int, text: String) {
        val progress = newProgress.coerceAtMost(binding.progressBar.max)
        binding.instructionText.text = text
        binding.percentText.text = context.getString(R.string.image_recommendations_task_daily_amount, newProgress, max)

        val anim = ProgressBarAnimation(oldProgress.toFloat(), progress.toFloat())
        anim.duration = 500
        binding.progressBar.startAnimation(anim)
    }

    inner class ProgressBarAnimation(var from: Float, var to: Float) : Animation() {
        init {
            from *= 100
            to *= 100
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            val value = from + (to - from) * interpolatedTime
            binding.progressBar.progress = value.toInt()
        }
    }
}
