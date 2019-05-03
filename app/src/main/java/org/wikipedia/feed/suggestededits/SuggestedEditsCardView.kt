package org.wikipedia.feed.suggestededits


import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.View
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_suggested_edits_add_descriptions_item.view.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ItemTouchHelperSwipeAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), ItemTouchHelperSwipeAdapter.SwipeableView, SuggestedEditsFeedClient.Callback {
    interface Callback {
        fun onSuggestedEditsCardClick(view: SuggestedEditsCardView)
    }

    private val disposables = CompositeDisposable()
    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    var isTranslation: Boolean = false
    var sourceSummary: RbPageSummary? = null
    var targetSummary: RbPageSummary? = null

    init {
        inflate(getContext(), R.layout.view_suggested_edit_card, this)
    }

    override fun setCard(@NonNull card: SuggestedEditsCard) {
        super.setCard(card)

        isTranslation = card.isTranslation
        sourceSummary = card.sourceSummary
        targetSummary = card.targetSummary

        setLayoutDirectionByWikiSite(WikiSite.forLanguageCode(sourceSummary!!.lang), this)

        cardView.setOnClickListener {
            if (callback != null && card != null) {
                callback!!.onSuggestedEditsCardClick(this)
            }
        }
        viewArticleSubtitle.visibility = View.GONE
        header(card)
        updateContents()
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        headerView.setCallback(callback)
    }

    private fun updateContents() {
        if (isTranslation) {
            sourceDescription = StringUtils.capitalize(sourceSummary!!.description)
            viewArticleSubtitle.visibility = View.VISIBLE
            viewArticleSubtitle.text = sourceDescription
        }
        viewArticleTitle.text = sourceSummary!!.normalizedTitle
        callToActionText.text = if (isTranslation) String.format(context.getString(R.string.add_translation), app.language().getAppLanguageCanonicalName(targetSummary!!.lang)) else context.getString(R.string.suggested_edits_add_description_button)
        showImageOrExtract()
    }

    private fun showImageOrExtract() {
        if (TextUtils.isEmpty(sourceSummary!!.thumbnailUrl)) {
            viewArticleImage.visibility = View.GONE
            viewArticleExtract.visibility = View.VISIBLE
            divider.visibility = View.VISIBLE
            viewArticleExtract.text = StringUtil.fromHtml(sourceSummary!!.extractHtml)
            viewArticleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            viewArticleImage.visibility = View.VISIBLE
            viewArticleExtract.visibility = View.GONE
            divider.visibility = View.GONE
            viewArticleImage.loadImage(Uri.parse(sourceSummary!!.thumbnailUrl))
        }
    }

    override fun onDetachedFromWindow() {
        disposables.clear()
        super.onDetachedFromWindow()
    }

    private fun header(card: SuggestedEditsCard) {
        headerView!!.setTitle(card.title())
                .setSubtitle(card.subtitle())
                .setImage(R.drawable.ic_mode_edit_white_24dp)
                .setImageCircleColor(R.color.base30)
                .setLangCode(if (isTranslation) card.wikiSite().languageCode() else "")
                .setCard(card)
                .setCallback(callback)
    }

    fun refreshCardContent() {
        SuggestedEditsFeedClient(isTranslation).getArticleWithMissingDescription(null, this)
    }

    override fun updateCardContent(card: SuggestedEditsCard) {
        setCard(card)
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 6
    }
}
