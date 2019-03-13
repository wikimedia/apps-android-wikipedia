package org.wikipedia.editactionfeed

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER_VERTICAL
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_my_contributions_progress.view.*
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil

class MyContributionsProgressView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_my_contributions_progress, this)
        isBaselineAligned = false
        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL
    }

    fun update(editCount: Int) {
        usernameText.text = AccountUtil.getUserName()
        contributionsText.text = resources.getQuantityString(R.plurals.edit_action_contribution_count, editCount)
    }
}
