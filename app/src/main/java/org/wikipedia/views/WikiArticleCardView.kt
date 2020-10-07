package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.util.Pair
import kotlinx.android.synthetic.main.view_wiki_article_card.view.*
import org.wikipedia.R
import org.wikipedia.util.StringUtil

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
        return imageContainer
    }

    fun getImageView(): FaceAndColorDetectImageView {
        return imageView
    }

    fun setExtract(extract: String?) {
        articleExtract.text = StringUtil.fromHtml(extract)
    }

    fun getSharedElements(): Array<Pair<View, String>> {
        return arrayOf(Pair(imageView, imageView.transitionName),
                Pair(articleTitle, articleTitle.transitionName),
                Pair(articleDescription, articleDescription.transitionName),
                Pair(articleExtract, articleExtract.transitionName))
    }
}
