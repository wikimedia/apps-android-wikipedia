package org.wikipedia.feed.featured

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import org.wikipedia.databinding.ViewCardFeaturedArticleBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.SiteInfoClient.getMainPageForLang
import org.wikipedia.views.ImageZoomHelper.Companion.isZooming
import org.wikipedia.views.ImageZoomHelper.Companion.setViewZoomable

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
            if (isZooming) {
                // Dispatch a fake CANCEL event to the container view, so that the long-press ripple is cancelled.
                binding.viewFeaturedArticleCardContentContainer.dispatchTouchEvent(
                    MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
                )
            } else {
                card?.let {
                    LongPressMenu(view, true, object : LongPressMenu.Callback {
                        override fun onOpenLink(entry: HistoryEntry) {
                            callback?.onSelectPage(it, entry, false)
                        }

                        override fun onOpenInNewTab(entry: HistoryEntry) {
                            callback?.onSelectPage(it, entry, true)
                        }

                        override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                            callback?.onAddPageToList(entry, addToDefault)
                        }

                        override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                            callback?.onMovePageToList(page!!.listId, entry)
                        }
                    }).show(it.historyEntry())
                }
            }
            false
        }
    }

    override var card: FeaturedArticleCard? = null
        set(value) {
            field = value
            value?.let {
                setLayoutDirectionByWikiSite(it.wikiSite(), binding.viewFeaturedArticleCardContentContainer)
                val articleTitle = it.articleTitle()
                val articleSubtitle = it.articleSubtitle()
                val extract = it.extract()
                val imageUri = it.image()
                articleTitle(articleTitle)
                articleSubtitle(articleSubtitle)
                extract(extract)
                image(imageUri)
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
            .setLangCode(card.wikiSite().languageCode())
            .setCard(card)
            .setCallback(callback)
    }

    private fun footer(card: FeaturedArticleCard) {
        binding.viewFeaturedArticleCardFooter.callback = footerCallback
        binding.viewFeaturedArticleCardFooter.setFooterActionText(card.footerActionText(), card.wikiSite().languageCode())
    }

    private fun image(uri: Uri?) {
        binding.viewWikiArticleCard.setImageUri(uri, false)
        uri?.run {
            setViewZoomable(binding.viewWikiArticleCard.getImageView())
        }
    }

    open val footerCallback: CardFooterView.Callback?
        get() = CardFooterView.Callback {
            card?.let {
                callback?.onSelectPage(it, HistoryEntry(PageTitle(
                    getMainPageForLang(it.wikiSite().languageCode()), it.wikiSite()),
                    it.historyEntry().source), false
                )
            }
        }

    companion object {
        const val EXTRACT_MAX_LINES = 8
    }
}
