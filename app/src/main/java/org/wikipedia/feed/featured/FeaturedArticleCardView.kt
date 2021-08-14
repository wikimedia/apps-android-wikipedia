package org.wikipedia.feed.featured

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import org.wikipedia.databinding.ViewCardFeaturedArticleBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.views.ImageZoomHelper

@Suppress("LeakingThis")
open class FeaturedArticleCardView(context: Context) : DefaultFeedCardView<FeaturedArticleCard>(context) {

    private val binding = ViewCardFeaturedArticleBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.viewFeaturedArticleCardContentContainer.setOnClickListener {
            card?.let {
                callback?.onSelectPage(it, it.historyEntry(), binding.viewWikiArticleCard.getSharedElements())
            }
        }

        binding.viewFeaturedArticleCardContentContainer.setOnLongClickListener { view ->
            if (ImageZoomHelper.isZooming) {
                ImageZoomHelper.dispatchCancelEvent(binding.viewFeaturedArticleCardContentContainer)
            } else {
                LongPressMenu(view, true, object : LongPressMenu.Callback {
                    override fun onOpenLink(entry: HistoryEntry) {
                        card?.let {
                            callback?.onSelectPage(it, entry, false)
                        }
                    }

                    override fun onOpenInNewTab(entry: HistoryEntry) {
                        card?.let {
                            callback?.onSelectPage(it, entry, true)
                        }
                    }

                    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                        callback?.onAddPageToList(entry, addToDefault)
                    }

                    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                        callback?.onMovePageToList(page!!.listId, entry)
                    }
                }).show(card?.historyEntry())
            }
            false
        }
    }

    override var card: FeaturedArticleCard? = null
        set(value) {
            field = value
            value?.let {
                setLayoutDirectionByWikiSite(it.wikiSite(), binding.viewFeaturedArticleCardContentContainer)
                articleTitle(it.articleTitle())
                articleSubtitle(it.articleSubtitle())
                extract(it.extract())
                image(it.image())
                header(it)
                footer(it)
            }
        }

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.viewFeaturedArticleCardHeader.setCallback(value)
        }

    private fun articleTitle(articleTitle: String) {
        binding.viewWikiArticleCard.setTitle(articleTitle)
    }

    private fun articleSubtitle(articleSubtitle: String?) {
        binding.viewWikiArticleCard.setDescription(articleSubtitle)
    }

    private fun extract(extract: String?) {
        binding.viewWikiArticleCard.setExtract(extract, EXTRACT_MAX_LINES)
    }

    private fun header(card: FeaturedArticleCard) {
        binding.viewFeaturedArticleCardHeader.setTitle(card.title())
            .setLangCode(card.wikiSite().languageCode)
            .setCard(card)
            .setCallback(callback)
    }

    private fun footer(card: FeaturedArticleCard) {
        binding.viewFeaturedArticleCardFooter.callback = footerCallback
        binding.viewFeaturedArticleCardFooter.setFooterActionText(card.footerActionText(), card.wikiSite().languageCode)
    }

    private fun image(uri: Uri?) {
        binding.viewWikiArticleCard.setImageUri(uri, false)
        uri?.run {
            ImageZoomHelper.setViewZoomable(binding.viewWikiArticleCard.getImageView())
        }
    }

    open val footerCallback: CardFooterView.Callback?
        get() = CardFooterView.Callback {
            card?.let {
                callback?.onSelectPage(it, HistoryEntry(PageTitle(
                    SiteInfoClient.getMainPageForLang(it.wikiSite().languageCode), it.wikiSite()),
                    it.historyEntry().source), false
                )
            }
        }

    companion object {
        const val EXTRACT_MAX_LINES = 8
    }
}
