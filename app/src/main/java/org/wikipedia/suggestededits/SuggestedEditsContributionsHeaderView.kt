package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_contributions_header.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.Contribution.Companion.EDIT_TYPE_IMAGE_TAG

class SuggestedEditsContributionsHeaderView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), SuggestedEditsTypeItem.Callback {
    interface Callback {
        fun onTypeItemClick(editType: Int)
    }

    var callback: Callback? = null
    private var filterViews = ArrayList<SuggestedEditsTypeItem>()

    init {
        View.inflate(context, R.layout.view_contributions_header, this)
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        filterViews.add(allTypesView)
        filterViews.add(articleDescriptionView)
        filterViews.add(imageCaptionsView)
        filterViews.add(imageTagsView)

        allTypesView.setAttributes(getContext().getString(R.string.edit_type_all), R.drawable.ic_mode_edit_themed_24dp, Contribution.EDIT_TYPE_GENERIC, this)
        articleDescriptionView.setAttributes(getContext().getString(R.string.description_edit_tutorial_title_descriptions), R.drawable.ic_article_description, Contribution.EDIT_TYPE_ARTICLE_DESCRIPTION, this)
        imageCaptionsView.setAttributes(getContext().getString(R.string.suggested_edits_image_captions), R.drawable.ic_image_caption, EDIT_TYPE_IMAGE_CAPTION, this)
        imageTagsView.setAttributes(getContext().getString(R.string.suggested_edits_image_tags), R.drawable.ic_image_tag, EDIT_TYPE_IMAGE_TAG, this)
    }

    fun updateFilterViewUI(editType: Int, totalContributions: Int) {
        val view: SuggestedEditsTypeItem
        val count: Int
        when (editType) {
            Contribution.EDIT_TYPE_ARTICLE_DESCRIPTION -> {
                view = articleDescriptionView
                count = SuggestedEditsUserStats.totalDescriptionEdits
            }
            EDIT_TYPE_IMAGE_CAPTION -> {
                view = imageCaptionsView
                count = SuggestedEditsUserStats.totalImageCaptionEdits
            }
            EDIT_TYPE_IMAGE_TAG -> {
                view = imageTagsView
                count = SuggestedEditsUserStats.totalImageTagEdits
            }
            else -> {
                view = allTypesView
                count = totalContributions
            }
        }
        contributionsCountText.text = context.getString(R.string.suggested_edits_contribution_type_title, count, resources.getQuantityString(R.plurals.suggested_edits_contribution, count))
        for (filterView in filterViews) {
            if (filterView == view) {
                filterView.setEnabledStateUI()
            } else {
                filterView.setDisabledStateUI()
            }
        }
    }

    fun updateTotalPageViews(pageViews: Long) {
        if (pageViews > 0) {
            contributionsSeenText.text = context.getString(R.string.suggested_edits_contribution_seen_text, pageViews.toString())
        }
    }

    override fun onTypeItemClick(editType: Int) {
        callback?.onTypeItemClick(editType)
    }
}