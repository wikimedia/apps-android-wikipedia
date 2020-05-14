package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.item_suggested_edits_contributions.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_CAPTION
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution.Companion.EDIT_TYPE_IMAGE_TAG
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsContributionsItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onClick()
    }

    var callback: Callback? = null

    fun setTitle(contributionTitle: String?) {
        title.text = StringUtil.fromHtml(contributionTitle)
    }

    fun setDescription(contributionDescription: String?) {
        description.text = StringUtil.fromHtml(contributionDescription)
    }

    fun setPageViewCountText(pageViewCount: Long) {
        if (pageViewCount == 0L) {
            pageViewLayout.visibility = View.GONE
        } else {
            pageViewLayout.visibility = View.VISIBLE
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
        if (url.isNullOrEmpty() || url == "null") {
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
        setOnClickListener {
            if (callback != null) {
                callback!!.onClick()
            }
        }
    }
}