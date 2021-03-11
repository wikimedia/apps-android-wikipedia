package org.wikipedia.userprofile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import org.wikipedia.databinding.ViewSuggestedEditsContributionDiffDetailBinding

class ContributionDiffDetailView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ViewSuggestedEditsContributionDiffDetailBinding.inflate(LayoutInflater.from(context), this, true)

    fun setLabelAndDetail(labelText: String? = "", detailText: String? = "", @DrawableRes drawableRes: Int = -1) {
        if (detailText.isNullOrEmpty()) {
            visibility = GONE
            return
        }
        binding.label.text = labelText
        binding.detail.text = detailText
        if (drawableRes != -1) {
            binding.icon.visibility = VISIBLE
            binding.icon.setImageResource(drawableRes)
        }
    }
}
