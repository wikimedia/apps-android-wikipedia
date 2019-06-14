package org.wikipedia.feed.suggestededits


import android.content.Context
import android.net.Uri
import android.view.View
import io.reactivex.annotations.NonNull
import kotlinx.android.synthetic.main.view_suggested_edit_card.view.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ItemTouchHelperSwipeAdapter

class SuggestedEditsCardView(context: Context) : DefaultFeedCardView<SuggestedEditsCard>(context), ItemTouchHelperSwipeAdapter.SwipeableView, SuggestedEditsFeedClient.Callback {
    interface Callback {
        fun onSuggestedEditsCardClick(view: SuggestedEditsCardView)
    }

    private var sourceDescription: String = ""
    private val app = WikipediaApp.getInstance()
    var isTranslation: Boolean = false
    var sourceSummary: SuggestedEditsSummary? = null
    var targetSummary: SuggestedEditsSummary? = null

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
            if (callback != null) {
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
            sourceDescription = sourceSummary!!.description!!.capitalize()
            viewArticleSubtitle.visibility = View.VISIBLE
            viewArticleSubtitle.text = sourceDescription
        }
        viewArticleTitle.text = sourceSummary!!.normalizedTitle
        callToActionText.text = if (isTranslation) context.getString(R.string.suggested_edits_feed_card_add_translation_in_language_button, app.language().getAppLanguageCanonicalName(targetSummary!!.lang)) else context.getString(R.string.suggested_edits_add_description_button)
        showImageOrExtract()
    }

    private fun showImageOrExtract() {
        if (sourceSummary!!.thumbnailUrl.isNullOrBlank()) {
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
