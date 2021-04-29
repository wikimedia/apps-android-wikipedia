package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewSuggestedEditsTaskItemBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.WikiCardView

internal class SuggestedEditsTaskView constructor(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {
    private val binding = ViewSuggestedEditsTaskItemBinding.inflate(LayoutInflater.from(context), this)

    init {
        val params = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val marginX = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()
        val marginY = DimenUtil.roundedDpToPx(8f)
        params.setMargins(marginX, marginY, marginX, marginY)
        layoutParams = params
    }

    private fun updateTranslateActionUI() {
        val color = ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color)
        binding.secondaryButton.iconTint = ColorStateList.valueOf(color)
        binding.secondaryButton.setTextColor(color)
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        updateTranslateActionUI()
        binding.taskTitle.text = task.title
        binding.taskDescription.text = task.description
        binding.primaryButton.text = task.primaryAction
        if (task.primaryActionIcon != 0) {
            binding.primaryButton.setIconResource(task.primaryActionIcon)
            binding.primaryButton.iconSize = DimenUtil.roundedDpToPx(16f)
            binding.primaryButton.iconPadding = DimenUtil.roundedDpToPx(8f)
        }
        binding.taskIcon.setImageResource(task.imageDrawable)
        binding.taskTitleNewLabel.visibility = if (task.new) VISIBLE else GONE
        binding.taskDailyProgress?.visibility = if (!task.new && task.dailyProgressMax > 0) VISIBLE else GONE
        if (task.dailyProgressMax > 0) {
            binding.taskDailyProgress?.update(task.dailyProgress, task.dailyProgressMax)
        }

        setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        binding.primaryButton.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        binding.secondaryButton.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, true)
            }
        }
        binding.secondaryButton.visibility = if (task.secondaryAction.isNullOrEmpty()) View.GONE else VISIBLE
        binding.secondaryButton.text = task.secondaryAction
    }

    interface Callback {
        fun onViewClick(task: SuggestedEditsTask, secondary: Boolean)
    }
}
