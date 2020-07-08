package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import kotlinx.android.synthetic.main.view_suggested_edits_contribution_diff_detail.view.*
import org.wikipedia.R

class EditsDiffDetailView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.view_suggested_edits_contribution_diff_detail, this, true)
    }

    fun setLabelAndDetail(labelText: String? = "", detailText: String? = "", @DrawableRes drawableRes: Int = -1) {
        if (detailText.isNullOrEmpty()) {
            visibility = GONE
            return
        }
        label.text = labelText
        detail.text = detailText
        if (drawableRes != -1) {
            icon.visibility = VISIBLE
            icon.setImageResource(drawableRes)
        }
    }
}
