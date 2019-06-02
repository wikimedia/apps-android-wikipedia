package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

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
        viewArticleTitle!!.text = StringUtil.fromHtml(pageSummary.displayTitle)

        if (pageSummary.thumbnailUrl.isNullOrEmpty()) {
            viewArticleImage!!.visibility = GONE
        } else {
            viewArticleImage!!.visibility = VISIBLE
            viewArticleImage!!.loadImage(Uri.parse(pageSummary.thumbnailUrl))
        }
        viewImageThumbnail.visibility = View.GONE
        viewReadButton.visibility = View.VISIBLE
        show()
    }

    fun setImageDetails(thumbUrl: String, fileName: String) {
        setConditionalLayoutDirection(this, WikipediaApp.getInstance().language().appLanguageCode)
        viewArticleTitle!!.text = StringUtil.fromHtml(fileName)

        val padding: Int = DimenUtil.dpToPx(8.0f).toInt()
        viewArticleImage!!.visibility = VISIBLE
        viewArticleImage!!.setPadding(padding, padding, padding, padding)
        viewArticleImage!!.setColorFilter(ResourceUtil.getThemedColor(context, R.attr.main_toolbar_icon_color))
        viewArticleImage.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp)

        if (thumbUrl.isEmpty()) {
            viewImageThumbnail!!.visibility = GONE
        } else {
            viewImageThumbnail.visibility = View.VISIBLE
            viewImageThumbnail!!.loadImage(Uri.parse(thumbUrl))
        }

        viewReadButton.visibility = View.GONE
        show()
    }

}
