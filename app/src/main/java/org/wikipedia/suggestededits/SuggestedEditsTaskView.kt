package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_suggested_edits_task_item.view.*
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.ResourceUtil

internal class SuggestedEditsTaskView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task_item, this)
        setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color))
    }

    private fun updateTranslateActionUI() {
        suggetedEditsTranslateImage.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
        suggetedEditsTranslateActionText.setTextColor(ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        updateTranslateActionUI()
        taskTitle.text = task.title!!
        taskDescription.text = task.description!!
        taskIcon.setImageResource(task.imageDrawable)

        addContainer.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, false)
            }
        }
        translateContainer.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task, true)
            }
        }
    }

    interface Callback {
        fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean)
    }
}
