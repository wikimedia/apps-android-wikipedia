package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_suggested_edits_task.view.*
import org.wikipedia.R

internal class SuggestedEditsTaskView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_task, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setUpViews(task: SuggestedEditsTask, callback: Callback?) {
        taskTitle.text = task.title
        taskDescription.text = task.description
        taskImage.visibility = if (task.showImagePlaceholder) View.VISIBLE else View.GONE
        taskImage.setImageDrawable(task.imageDrawable)
        taskInfoContainer.alpha = if (task.disabled) 0.56f else 1.0f
        unlockMessageContainer.visibility = if (task.disabled) View.VISIBLE else View.GONE
        unlockMessageText.text = task.unlockMessageText
        unlockActionsContainer.visibility = if (task.disabled) View.GONE else View.VISIBLE
        unlockActionPositiveButton.text = task.unlockActionPositiveButtonString
        unlockActionNegativeButton.text = task.unlockActionNegativeButtonString
        taskActionLayout.visibility = if (task.showActionLayout) View.VISIBLE else View.GONE

        taskInfoContainer.setOnClickListener {
            if (!task.disabled) {
                callback?.onViewClick(task)
            }
        }

        unlockActionPositiveButton.setOnClickListener {
            callback?.onPositiveActionClick(task)
        }

        unlockActionNegativeButton.setOnClickListener {
            callback?.onNegativeActionClick(task)
        }
    }

    interface Callback {
        fun onPositiveActionClick(task: SuggestedEditsTask)
        fun onNegativeActionClick(task: SuggestedEditsTask)
        fun onViewClick(task: SuggestedEditsTask)
    }
}
