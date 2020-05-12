package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_suggested_edits_contribution_diff_detail.view.*
import org.wikipedia.R

class SuggestedEditsDiffDetailView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.view_suggested_edits_contribution_diff_detail, this, true)
    }

    fun setLabel(labelText: String?) {
        label.text = labelText
    }

    fun setDetail(detailText: String?) {
        detail.text = detailText
    }
}