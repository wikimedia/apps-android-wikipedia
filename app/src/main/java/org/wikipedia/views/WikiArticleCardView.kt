package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.util.Pair
import kotlinx.android.synthetic.main.view_wiki_article_card.view.*
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil

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
        return imageContainer
    }

    fun getImageView(): FaceAndColorDetectImageView {
        return imageView
    }

    fun setExtract(extract: String?) {
        articleExtract.text = StringUtil.fromHtml(extract)
    }

    fun getSharedElements(): Array<Pair<View, String>> {
        val shareElements: MutableList<Pair<View, String>> = mutableListOf(Pair(articleTitle, articleTitle.transitionName),
                Pair(articleDivider, articleDivider.transitionName))

        if (!DimenUtil.isLandscape(context)) {
            shareElements.add(Pair(imageView, imageView.transitionName))
        }

        if (articleDescription.text.isNotEmpty()) {
            shareElements.add(Pair(articleDescription, articleDescription.transitionName))
        }
        return shareElements.toTypedArray()
    }
}
