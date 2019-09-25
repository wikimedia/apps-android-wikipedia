package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_suggested_edits_task_item.view.*
import org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

internal class SuggestedEditsTaskView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task_item, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        taskImageDetailView.setImageParams(DimenUtil.roundedDpToPx(24.0f), DimenUtil.roundedDpToPx(24.0f))
        taskImageDetailView.setImageBackgroundParams(DimenUtil.roundedDpToPx(48.0f), DimenUtil.roundedDpToPx(48.0f))
    }

    fun updateTranslateActionUI() {
        suggetedEditsTranslateImage.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
        suggetedEditsTranslateActionText.setTextColor(ResourceUtil.getThemedColor(context, if (WikipediaApp.getInstance().language().appLanguageCodes.size >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION)
            R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        updateTranslateActionUI()
        taskImageDetailView.setTitle(task.title!!)
        taskImageDetailView.setDescription(task.description!!)
        taskImageDetailView.setImageDrawable(task.imageDrawable!!)
        taskImageDetailView.setImageTint(ResourceUtil.getThemedAttributeId(context!!, R.attr.themed_icon_color))

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
