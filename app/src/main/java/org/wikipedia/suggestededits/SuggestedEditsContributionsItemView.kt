package org.wikipedia.suggestededits

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import butterknife.OnClick
import kotlinx.android.synthetic.main.item_suggested_edits_contirbutions.view.*
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsContributionsItemView<T>(context: Context) : LinearLayout(context) {
    interface Callback<T> {
        fun onClick()
    }

    private var callback: Callback<T>? = null
    private var item: T? = null
    fun setItem(item: T?) {
        this.item = item
    }

    fun setCallback(callback: Callback<T>?) {
        this.callback = callback
    }


    fun setTitle(contributionTitle: String?) {
        title.text = contributionTitle
    }


    fun setDescription(contributionDescription: String?) {
        description.text = contributionDescription
    }

    fun setTime(contributionTime: String?) {
        time.text = contributionTime
    }

    fun setTagType(contributionTagType: String?) {
        tagType.text = contributionTagType
    }


    fun setImageUrl(url: String?) {
        if (url == null || url.equals("null")) {
            image.visibility = View.GONE
            return
        }
        //Glide.with(this).load(url).into(image);
        ViewUtil.loadImageWithRoundedCorners(image, "https://upload.wikimedia.org/wikipedia/commons/5/53/1-sucre-1930.jpg")
    }


    @OnClick
    fun onClick() {
        if (callback != null) {
            callback!!.onClick()
        }
    }


    init {
        View.inflate(context, R.layout.item_suggested_edits_contirbutions, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val topBottomPadding = 16
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding.toFloat()), 0, DimenUtil.roundedDpToPx(topBottomPadding.toFloat()))
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
    }
}
