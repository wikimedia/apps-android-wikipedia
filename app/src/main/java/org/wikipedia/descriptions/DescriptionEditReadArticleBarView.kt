package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil


class DescriptionEditReadArticleBarView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : ConstraintLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_description_edit_read_article_bar, this)
        hide()
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setPageSummary(pageSummary: RbPageSummary) {
        setConditionalLayoutDirection(this, pageSummary.lang)
        val horizontalMargin: Int = DimenUtil.dpToPx(16.0f).toInt()
        val params: MarginLayoutParams = viewArticleTitle!!.layoutParams as MarginLayoutParams
        params.setMargins(horizontalMargin, 0, 0, 0)
        viewArticleTitle!!.layoutParams = params
        viewArticleTitle!!.requestLayout()
        viewArticleTitle!!.text = StringUtil.fromHtml(pageSummary.displayTitle)

        if (pageSummary.thumbnailUrl.isNullOrEmpty()) {
            viewArticleImage!!.visibility = GONE
        } else {
            val verticalMargin: Int = DimenUtil.dpToPx(8.0f).toInt()
            val layoutparams: MarginLayoutParams = viewArticleImage.layoutParams as MarginLayoutParams
            layoutparams.setMargins(horizontalMargin, verticalMargin, 0, verticalMargin)
            viewArticleImage!!.layoutParams = layoutparams
            viewArticleImage!!.requestLayout()
            val padding: Int = DimenUtil.dpToPx(4.0f).toInt()
            viewArticleImage!!.setPadding(padding, padding, padding, padding)
            viewArticleImage!!.visibility = VISIBLE
            ViewUtil.loadImageUrlInto(viewArticleImage!!, pageSummary.thumbnailUrl)
        }

        viewImageThumbnail.visibility = View.GONE
        viewReadButton.visibility = View.VISIBLE
        show()
    }

    fun setImageDetails(thumbUrl: String, fileName: String) {
        //Todo: only for testing: set direction based on image details
        setConditionalLayoutDirection(this, WikipediaApp.getInstance().language().appLanguageCode)
        viewArticleTitle!!.text = StringUtil.fromHtml(fileName)

        val padding: Int = DimenUtil.dpToPx(12.0f).toInt()
        viewArticleImage!!.visibility = VISIBLE
        viewArticleImage!!.setPadding(padding, padding, padding, padding)
        viewArticleImage!!.setColorFilter(ResourceUtil.getThemedColor(context, R.attr.main_toolbar_icon_color))
        viewArticleImage.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp)

        if (thumbUrl.isEmpty()) {
            viewImageThumbnail!!.visibility = GONE
        } else {
            viewImageThumbnail.visibility = View.VISIBLE
            ViewUtil.loadImageUrlInto(viewImageThumbnail!!, thumbUrl)
        }

        viewReadButton.visibility = View.GONE
        show()
    }

}
