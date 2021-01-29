package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Pair
import kotlinx.android.synthetic.main.view_wiki_article_card.view.*
import org.wikipedia.R
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil

class WikiArticleCardView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.view_wiki_article_card, this)
    }

    fun setTitle(title: String) {
        articleTitle.text = StringUtil.fromHtml(title)
    }

    fun setDescription(description: String?) {
        articleDescription.text = description
    }

    fun getImageView(): FaceAndColorDetectImageView {
        return articleImage
    }

    fun setExtract(extract: String?, maxLines: Int) {
        articleExtract.text = StringUtil.fromHtml(extract)
        articleExtract.maxLines = maxLines
    }

    fun getSharedElements(): Array<Pair<View, String>> {
        return TransitionUtil.getSharedElements(context, articleTitle, articleDescription, articleImage)
    }

    fun setImageUri(uri: Uri?, hideInLandscape: Boolean = true) {
        if (uri == null || (DimenUtil.isLandscape(context) && hideInLandscape) || !Prefs.isImageDownloadEnabled()) {
            articleImageContainer.visibility = GONE
        } else {
            articleImageContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT,
                    DimenUtil.leadImageHeightForDevice(context) - DimenUtil.getToolbarHeightPx(context))
            articleImageContainer.visibility = VISIBLE
            articleImage.loadImage(uri)
        }
    }

    fun prepareForTransition(title: PageTitle) {
        setImageUri(if (TextUtils.isEmpty(title.thumbUrl)) null else Uri.parse(title.thumbUrl))

        setTitle(title.displayText)
        setDescription(title.description)
        articleDivider.visibility = View.GONE
        L10nUtil.setConditionalLayoutDirection(this, title.wikiSite.languageCode())
    }
}
