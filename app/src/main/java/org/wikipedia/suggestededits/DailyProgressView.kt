package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewSuggestedEditsDailyProgressBinding

class DailyProgressView : ConstraintLayout {

    var binding = ViewSuggestedEditsDailyProgressBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setBackgroundColor(ContextCompat.getColor(context, R.color.red90))
    }

    fun setMaximum(max: Int) {
        binding.progressBar.max = max
    }

    fun update(oldProgress: Int, newProgress: Int, text: String) {
        binding.instructionText.text = text
        binding.progressBar.progress = newProgress.coerceAtMost(binding.progressBar.max)
        binding.percentText.text = context.getString(R.string.text_size_percent, newProgress)
    }
}
