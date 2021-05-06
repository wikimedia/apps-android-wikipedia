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

class MainPageCardView(context: Context) : DefaultFeedCardView<MainPageCard?>(context) {

    private var binding = ViewStaticCardBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)
    }

    override fun setCard(card: MainPageCard) {
        super.setCard(card)
        binding.cardHeader.setTitle(getStringForArticleLanguage(getCard()!!.wikiSite().languageCode(), R.string.view_main_page_card_title))
                .setLangCode(getCard()!!.wikiSite().languageCode())
                .setCard(getCard()!!)
                .setCallback(callback)
        binding.cardFooter.callback = object : CardFooterView.Callback {
            override fun onFooterClicked() {
                goToMainPage()
            }
        }
        binding.cardFooter.setFooterActionText(getStringForArticleLanguage(getCard()!!.wikiSite().languageCode(),
                R.string.view_main_page_card_action), getCard()!!.wikiSite().languageCode())
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        super.setCallback(callback)
        binding.cardHeader.setCallback(callback)
    }

    private fun goToMainPage() {
        card?.let {
            callback?.onSelectPage(
                it,
                HistoryEntry(
                    PageTitle(
                        getMainPageForLang(it.wikiSite().languageCode()),
                        it.wikiSite()
                    ),
                    HistoryEntry.SOURCE_FEED_MAIN_PAGE
                ), false
            )
        }
    }
}
