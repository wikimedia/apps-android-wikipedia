package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.util.Pair
import kotlinx.android.synthetic.main.view_wiki_article_card.view.*
import org.wikipedia.R
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil

class WikiArticleCardView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.view_wiki_article_card, this)
    }

    fun setTitle(title: String) {
        articleTitle.text = StringUtil.fromHtml(title)
    }

    fun setDescription(description: String?) {
        articleDescription.text = description
    }

    fun getImageContainer(): View {
        return articleImageContainer
    }

    fun getImageView(): FaceAndColorDetectImageView {
        return articleImage
    }

    fun setExtract(extract: String?, maxLines: Int) {
        articleExtract.text = StringUtil.fromHtml(extract)
        articleExtract.maxLines = maxLines
    }

    fun getSharedElements(): Array<Pair<View, String>> {
        return TransitionUtil.getSharedElements(context, articleTitle, articleDescription, articleImage, articleDivider)
    }

    fun prepareForTransition(title: PageTitle) {
        val uri = if (TextUtils.isEmpty(title.thumbUrl)) null else Uri.parse(title.thumbUrl)
        if (uri == null || DimenUtil.isLandscape(context) || !Prefs.isImageDownloadEnabled()) {
            articleImageContainer.visibility = GONE
        } else {
            articleImageContainer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    DimenUtil.leadImageHeightForDevice(context) - DimenUtil.getToolbarHeightPx(context))
            articleImageContainer.visibility = VISIBLE
            articleImage.loadImage(uri)
        }

        setTitle(title.displayText)
        setDescription(title.description)
        L10nUtil.setConditionalLayoutDirection(this, title.wikiSite.languageCode())
    }
}
