package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.databinding.ViewDescriptionEditReadArticleBarBinding
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class DescriptionEditBottomBarView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    private val binding = ViewDescriptionEditReadArticleBarBinding.inflate(LayoutInflater.from(context), this)

    init {
        hide()
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setSummary(summaryForEdit: PageSummaryForEdit) {
        setConditionalLayoutDirection(this, summaryForEdit.lang)
        binding.viewArticleTitle.text = StringUtil.fromHtml(StringUtil.removeNamespace(summaryForEdit.displayTitle!!))
        if (summaryForEdit.thumbnailUrl.isNullOrEmpty()) {
            binding.viewImageThumbnail.visibility = GONE
        } else {
            binding.viewImageThumbnail.visibility = VISIBLE
            ViewUtil.loadImage(binding.viewImageThumbnail, summaryForEdit.thumbnailUrl)
        }
        show()
    }
}
