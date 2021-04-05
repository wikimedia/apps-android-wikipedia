package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewSuggestedEditsDailyProgressSmallBinding

class DailyProgressViewSmall : ConstraintLayout {
    var binding = ViewSuggestedEditsDailyProgressSmallBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun update(newProgress: Int, max: Int) {
        binding.percentText.text = context.getString(R.string.image_recommendations_task_daily_amount, newProgress, max)
    }
}
