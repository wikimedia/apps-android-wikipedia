package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_suggested_edits_tasks.*
import kotlinx.android.synthetic.main.view_suggested_edits_task_item.view.*
import org.wikipedia.R
import org.wikipedia.util.DimenUtil

internal class SuggestedEditsTaskView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task_item, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        taskImageDetailView.setImageParams(DimenUtil.roundedDpToPx(24.0f), DimenUtil.roundedDpToPx(24.0f))
        taskImageDetailView.setImageBackgroundParams(DimenUtil.roundedDpToPx(48.0f), DimenUtil.roundedDpToPx(48.0f))
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        taskImageDetailView.setTitle(task.title!!)
        taskImageDetailView.setDescription(task.description!!)
        taskImageDetailView.setImageDrawable(task.imageDrawable!!)
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
