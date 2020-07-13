package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.view_suggested_edits_task_item.view.*
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.WikiCardView

internal class SuggestedEditsTaskView constructor(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task_item, this)
        val params = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val marginX = resources.getDimension(R.dimen.activity_horizontal_margin).toInt()
        val marginY = DimenUtil.roundedDpToPx(8f)
        params.setMargins(marginX, marginY, marginX, marginY)
        layoutParams = params
    }

    private fun updateTranslateActionUI() {
        val color = ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color)
        translateButton.iconTint = ColorStateList.valueOf(color)
        translateButton.setTextColor(color)
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        updateTranslateActionUI()
        taskTitle.text = task.title
        taskDescription.text = task.description
        taskIcon.setImageResource(task.imageDrawable)
        taskTitleNewLabel.visibility = if (task.new) View.VISIBLE else GONE

        clickContainer.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        addButton.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        translateButton.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, true)
            }
        }
        translateButton.visibility = if (task.translatable) View.VISIBLE else GONE
    }

    interface Callback {
        fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean)
    }
}
