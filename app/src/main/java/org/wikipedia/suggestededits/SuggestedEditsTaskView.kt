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
        binding.translateButton.iconTint = ColorStateList.valueOf(color)
        binding.translateButton.setTextColor(color)
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        updateTranslateActionUI()
        binding.taskTitle.text = task.title
        binding.taskDescription.text = task.description
        binding.taskIcon.setImageResource(task.imageDrawable)
        binding.taskTitleNewLabel.visibility = if (task.new) View.VISIBLE else GONE

        setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        binding.addButton.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        binding.translateButton.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, true)
            }
        }
        binding.translateButton.visibility = if (task.translatable) View.VISIBLE else GONE
    }

    interface Callback {
        fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean)
    }
}
