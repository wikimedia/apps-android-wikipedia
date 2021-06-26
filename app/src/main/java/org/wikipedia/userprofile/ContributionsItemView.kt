package org.wikipedia.userprofile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePaddingRelative
import org.wikipedia.R
import org.wikipedia.databinding.ItemSuggestedEditsContributionsBinding
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil
import java.text.DecimalFormat
import kotlin.math.abs

class ContributionsItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ItemSuggestedEditsContributionsBinding.inflate(LayoutInflater.from(context), this)
    private var numFormat: DecimalFormat = DecimalFormat("+0;-#")
    var callback: Callback? = null
    var contribution: Contribution? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        updatePaddingRelative(start = DimenUtil.roundedDpToPx(16f))
        background = ContextCompat.getDrawable(getContext(), ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground))
        setOnClickListener {
            if (binding.contributionTitle.text.isNotEmpty()) {
                callback?.onClick(context, contribution!!)
            }
        }
    }

    fun setTitle(contributionTitle: String?) {
        binding.contributionTitle.text = StringUtil.fromHtml(contributionTitle)
    }

    fun setDescription(contributionDescription: String?) {
        binding.contributionDescription.text = StringUtil.fromHtml(contributionDescription)
    }

    fun setPageViewCountText(pageViewCount: Long) {
        if (pageViewCount == 0L) {
            binding.pageViewImage.visibility = GONE
            binding.pageviewCountText.visibility = GONE
        } else {
            binding.pageViewImage.visibility = VISIBLE
            binding.pageviewCountText.visibility = VISIBLE
            binding.pageviewCountText.text = context.getString(R.string.suggested_edits_contribution_views, pageViewCount.toString())
        }
    }

    fun setIcon(contributionType: Int) {
        when (contributionType) {
            EDIT_TYPE_IMAGE_CAPTION -> {
                binding.contributionIcon.setImageResource(R.drawable.ic_image_caption)
            }
            EDIT_TYPE_IMAGE_TAG -> {
                binding.contributionIcon.setImageResource(R.drawable.ic_image_tag)
            }
            else -> {
                binding.contributionIcon.setImageResource(R.drawable.ic_article_description)
            }
        }
    }

    fun setImageUrl(url: String?) {
        if (url.isNullOrEmpty() || url == "null") {
            binding.contributionImage.setImageDrawable(null)
        } else {
            binding.contributionImage.visibility = VISIBLE
            ViewUtil.loadImageWithRoundedCorners(binding.contributionImage, url)
        }
    }

    fun setDiffCountText(contribution: Contribution) {
        if (contribution.editType == EDIT_TYPE_IMAGE_TAG) {
            binding.contributionDiffCountText.visibility = VISIBLE
            binding.contributionDiffCountText.text = resources.getQuantityString(R.plurals.suggested_edits_tags_diff_count_text, abs(contribution.tagCount), numFormat.format(contribution.tagCount))
            binding.contributionDiffCountText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.action_mode_green_background))
        } else {
            binding.contributionDiffCountText.visibility = VISIBLE
            binding.contributionDiffCountText.text = resources.getQuantityString(R.plurals.suggested_edits_contribution_diff_count_text, abs(contribution.sizeDiff), numFormat.format(contribution.sizeDiff))
            binding.contributionDiffCountText.setTextColor(if (contribution.sizeDiff < 0) ResourceUtil.getThemedColor(context, R.attr.colorError)
            else ResourceUtil.getThemedColor(context, R.attr.action_mode_green_background))
        }
    }

    interface Callback {
        fun onClick(context: Context, contribution: Contribution)
    }
}
