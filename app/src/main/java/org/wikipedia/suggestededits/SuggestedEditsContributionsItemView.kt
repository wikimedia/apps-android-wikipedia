package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_suggested_edits_contributions.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import java.text.DecimalFormat
import kotlin.math.abs

class SuggestedEditsContributionsItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var numFormat: DecimalFormat = DecimalFormat("+0;-#")
    var callback: Callback? = null
    var contribution: Contribution? = null

    init {
        View.inflate(context, R.layout.item_suggested_edits_contributions, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ViewCompat.setPaddingRelative(this, DimenUtil.roundedDpToPx(16f), 0, 0, 0)
        background = getContext().getDrawable(ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground))
        setOnClickListener {
            if (callback != null && title.text.isNotEmpty()) {
                callback?.onClick(context, contribution!!)
            }
        }
    }

    fun setTitle(contributionTitle: String?) {
        title.text = StringUtil.fromHtml(contributionTitle)
    }

    fun setDescription(contributionDescription: String?) {
        description.text = StringUtil.fromHtml(contributionDescription)
    }

    fun setPageViewCountText(pageViewCount: Long) {
        val noPages = pageViewCount == 0L
        pageViewImage.isVisible = !noPages
        pageviewCountText.isVisible = !noPages
        if (!noPages) {
            pageviewCountText.text = context.getString(R.string.suggested_edits_contribution_views, pageViewCount.toString())
        }
    }

    fun setIcon(contributionType: Int) {
        when (contributionType) {
            EDIT_TYPE_IMAGE_CAPTION -> {
                contributionIcon.setImageResource(R.drawable.ic_image_caption)
            }
            EDIT_TYPE_IMAGE_TAG -> {
                contributionIcon.setImageResource(R.drawable.ic_image_tag)
            }
            else -> {
                contributionIcon.setImageResource(R.drawable.ic_article_description)
            }
        }
    }

    fun setImageUrl(url: String?) {
        if (url.isNullOrEmpty() || url == "null") {
            image.setImageDrawable(null)
        } else {
            image.isVisible = false
            ViewUtil.loadImageWithRoundedCorners(image, url)
        }
    }

    fun setDiffCountText(contribution: Contribution) {
        contributionDiffCountText.isVisible = true
        if (contribution.editType == EDIT_TYPE_IMAGE_TAG) {
            contributionDiffCountText.text = resources.getQuantityString(R.plurals.suggested_edits_tags_diff_count_text, abs(contribution.tagCount), numFormat.format(contribution.tagCount))
            contributionDiffCountText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.action_mode_green_background))
        } else {
            contributionDiffCountText.text = resources.getQuantityString(R.plurals.suggested_edits_contribution_diff_count_text, abs(contribution.sizeDiff), numFormat.format(contribution.sizeDiff))
            contributionDiffCountText.setTextColor(if (contribution.sizeDiff < 0) ResourceUtil.getThemedColor(context, R.attr.colorError)
            else ResourceUtil.getThemedColor(context, R.attr.action_mode_green_background))
        }
    }

    interface Callback {
        fun onClick(context: Context, contribution: Contribution)
    }
}