package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.widget.ImageViewCompat
import kotlinx.android.synthetic.main.view_suggested_edits_task_item.view.*
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

internal class SuggestedEditsTaskView constructor(context: Context, attrs: AttributeSet? = null) : CardView(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task_item, this)
        val params = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val margin = DimenUtil.roundedDpToPx(16f)
        params.setMargins(margin, margin / 2, margin, margin / 2)
        layoutParams = params
        radius = DimenUtil.dpToPx(12f)
        cardElevation = DimenUtil.dpToPx(6f)
        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }

    private fun updateTranslateActionUI() {
        val color = ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color)
        ImageViewCompat.setImageTintList(suggestedEditsTranslateImage, ColorStateList.valueOf(color))
        suggestedEditsTranslateActionText.setTextColor(color)
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
        translateContainer.visibility = if (task.translatable) View.VISIBLE else GONE
    }

    interface Callback {
        fun onViewClick(task: SuggestedEditsTask, isTranslate: Boolean)
    }
}
