package org.wikipedia.suggestededits

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import kotlinx.android.synthetic.main.item_suggested_edits_contributions.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsContributionsItemView<T>(context: Context) : LinearLayout(context) {
    interface Callback {
        fun onClick(context: Context, contribution: Contribution)
    }

    private var callback: Callback? = null
    private var contribution: Contribution? = null

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }


    fun setTitle(contributionTitle: String?) {
        title.text = contributionTitle
    }

    fun setItem(contribution: Contribution) {
        this.contribution = contribution
    }


    fun setDescription(contributionDescription: String?) {
        description.text = contributionDescription
    }

    fun setTagType(contributionType: Int, language: String) {
        when (contributionType) {
            EDIT_TYPE_IMAGE_CAPTION -> {
                editType.text = context.getString(R.string.suggested_edits_contributions_type, context.getString(R.string.description_edit_add_caption_hint), language)
            }
            EDIT_TYPE_IMAGE_TAG -> {
                editType.text = context.getString(R.string.suggested_edits_contributions_type, context.getString(R.string.suggested_edits_type_image_tag), language)
            }
            else -> {
                editType.text = context.getString(R.string.suggested_edits_contributions_type, context.getString(R.string.description_edit_text_hint), language)
            }
        }
    }


    fun setImageUrl(url: String?) {
        if (url.isNullOrEmpty() || url.equals("null")) {
            image.visibility = View.GONE
            return
        } else {
            image.visibility = View.VISIBLE
            ViewUtil.loadImageWithRoundedCorners(image, url)
        }
    }

    init {
        View.inflate(context, R.layout.item_suggested_edits_contributions, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        setOnClickListener {
            if (callback != null) {
                callback!!.onClick(context, contribution!!)
            }
        }
    }
}
