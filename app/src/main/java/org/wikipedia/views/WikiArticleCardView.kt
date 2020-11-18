package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.util.Pair
import kotlinx.android.synthetic.main.view_wiki_article_card.view.*
import kotlinx.android.synthetic.main.view_wiki_article_card.view.articleExtract
import kotlinx.android.synthetic.main.view_wiki_article_card.view.articleTitle
import org.wikipedia.R
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil

class WikiArticleCardView constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    var isLoaded: Boolean = false

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

    fun setExtract(extract: String?) {
        articleExtract.text = StringUtil.fromHtml(extract)
    }

    fun getSharedElements(): Array<Pair<View, String>> {
        return TransitionUtil.getSharedElements(context, articleTitle, articleDescription, articleImage, articleDivider)
    }
}
