package org.wikipedia.userprofile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.wikipedia.R
import org.wikipedia.databinding.ViewContributionsHeaderBinding
import org.wikipedia.suggestededits.SuggestedEditsTypeItemView
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_ARTICLE_DESCRIPTION
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_IMAGE_TAG

class ContributionsHeaderView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), SuggestedEditsTypeItemView.Callback {

    private val binding = ViewContributionsHeaderBinding.inflate(LayoutInflater.from(context), this)
    private var filterViews: Array<SuggestedEditsTypeItemView>
    var callback: Callback? = null

    init {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        filterViews = arrayOf(binding.allTypesView, binding.articleDescriptionView, binding.imageCaptionsView, binding.imageTagsView)

        binding.allTypesView.setAttributes(getContext().getString(R.string.edit_type_all), R.drawable.ic_mode_edit_themed_24dp, Contribution.EDIT_TYPE_GENERIC, this)
        binding.articleDescriptionView.setAttributes(getContext().getString(R.string.description_edit_tutorial_title_descriptions), R.drawable.ic_article_description, Contribution.EDIT_TYPE_ARTICLE_DESCRIPTION, this)
        binding.imageCaptionsView.setAttributes(getContext().getString(R.string.suggested_edits_image_captions), R.drawable.ic_image_caption, EDIT_TYPE_IMAGE_CAPTION, this)
        binding.imageTagsView.setAttributes(getContext().getString(R.string.suggested_edits_image_tags), R.drawable.ic_image_tag, EDIT_TYPE_IMAGE_TAG, this)
    }

    override fun onTypeItemClick(editType: Int) {
        callback?.onTypeItemClick(editType)
    }

    fun updateFilterViewUI(editType: Int, totalContributions: Int) {
        val view = when (editType) {
            EDIT_TYPE_ARTICLE_DESCRIPTION -> binding.articleDescriptionView
            EDIT_TYPE_IMAGE_CAPTION -> binding.imageCaptionsView
            EDIT_TYPE_IMAGE_TAG -> binding.imageTagsView
            else -> binding.allTypesView
        }
        binding.contributionsCountText.text = context.getString(
            R.string.suggested_edits_contribution_type_title,
            totalContributions,
            resources.getQuantityString(R.plurals.suggested_edits_contribution, totalContributions)
        )
        filterViews.forEach {
            if (it == view) {
                it.setEnabledStateUI()
            } else {
                it.setDisabledStateUI()
            }
        }
    }

    fun updateTotalPageViews(pageViews: Long) {
        if (pageViews > 0) {
            binding.contributionsSeenText.text = context.resources.getQuantityString(R.plurals.suggested_edits_contribution_seen_times, pageViews.toInt(), pageViews)
        }
    }

    interface Callback {
        fun onTypeItemClick(editType: Int)
    }
}
