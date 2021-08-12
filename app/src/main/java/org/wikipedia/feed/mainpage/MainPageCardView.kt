package org.wikipedia.feed.mainpage

import android.content.Context
import android.view.LayoutInflater
import org.wikipedia.R
import org.wikipedia.databinding.ViewStaticCardBinding
import org.wikipedia.feed.view.CardFooterView
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfoClient.getMainPageForLang
import org.wikipedia.util.L10nUtil.getStringForArticleLanguage

class MainPageCardView(context: Context) : DefaultFeedCardView<MainPageCard>(context) {

    private val binding = ViewStaticCardBinding.inflate(LayoutInflater.from(context), this, true)

    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.cardHeader.setCallback(value)
        }

    override var card: MainPageCard? = null
        set(value) {
            field = value
            value?.let {
                binding.cardHeader.setTitle(getStringForArticleLanguage(it.wikiSite().languageCode, R.string.view_main_page_card_title))
                    .setLangCode(it.wikiSite().languageCode)
                    .setCard(it)
                    .setCallback(callback)
                binding.cardFooter.callback = CardFooterView.Callback { goToMainPage() }
                binding.cardFooter.setFooterActionText(getStringForArticleLanguage(it.wikiSite().languageCode,
                    R.string.view_main_page_card_action), it.wikiSite().languageCode)
            }
        }

    private fun goToMainPage() {
        card?.let {
            callback?.onSelectPage(it, HistoryEntry(PageTitle(getMainPageForLang(it.wikiSite().languageCode), it.wikiSite()),
                HistoryEntry.SOURCE_FEED_MAIN_PAGE), false)
        }
    }
}
