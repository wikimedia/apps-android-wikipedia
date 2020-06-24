package org.wikipedia.suggestededits

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
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
            if (callback != null) {
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
        if (pageViewCount == 0L) {
            pageViewImage.visibility = GONE
            pageviewCountText.visibility = GONE
        } else {
            pageViewImage.visibility = VISIBLE
            pageviewCountText.visibility = VISIBLE
            pageviewCountText.text = pageViewCount.toString()
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
        if (url == null) {
            image.visibility = INVISIBLE
            return
        }
        if (url.isEmpty() || url == "null") {
            image.visibility = GONE
        } else {
            image.visibility = VISIBLE
            ViewUtil.loadImageWithRoundedCorners(image, url)
        }
    }

    fun setDiffCountText(contribution: Contribution) {
        if (contribution.editType == EDIT_TYPE_IMAGE_TAG) {
            contributionDiffCountText.visibility = VISIBLE
            contributionDiffCountText.text = resources.getQuantityString(R.plurals.suggested_edits_tags_diff_count_text, abs(contribution.tagCount), numFormat.format(contribution.tagCount))
            contributionDiffCountText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.action_mode_green_background))
        } else {
            contributionDiffCountText.visibility = VISIBLE
            contributionDiffCountText.text = resources.getQuantityString(R.plurals.suggested_edits_contribution_diff_count_text, abs(contribution.sizeDiff), numFormat.format(contribution.sizeDiff))
            contributionDiffCountText.setTextColor(if (contribution.sizeDiff < 0) ResourceUtil.getThemedColor(context, R.attr.colorError)
            else ResourceUtil.getThemedColor(context, R.attr.action_mode_green_background))
        }
    }

    interface Callback {
        fun onClick(context: Context, contribution: Contribution)
    }
}