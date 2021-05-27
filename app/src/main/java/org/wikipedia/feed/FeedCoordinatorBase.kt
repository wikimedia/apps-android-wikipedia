package org.wikipedia.feed

import android.content.Context
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.accessibility.AccessibilityCard
import org.wikipedia.feed.announcement.AnnouncementClient
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.feed.mostread.MostReadListCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.offline.OfflineCard
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.progress.ProgressCard
import org.wikipedia.feed.suggestededits.SuggestedEditsFeedClient
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ThrowableUtil
import org.wikipedia.util.log.L
import java.util.*

abstract class FeedCoordinatorBase(private val context: Context) {

    interface FeedUpdateListener {
        fun insert(card: Card, pos: Int)
        fun remove(card: Card, pos: Int)
        fun finished(shouldUpdatePreviousCard: Boolean)
    }

    private var wiki: WikiSite? = null
    private var updateListener: FeedUpdateListener? = null
    private val pendingClients = mutableListOf<FeedClient>()
    private val callback = ClientRequestCallback()
    private val progressCard = ProgressCard()
    private var currentDayCardAge = -1
    private val hiddenCards =
        Collections.newSetFromMap(object : LinkedHashMap<String?, Boolean?>() {
            public override fun removeEldestEntry(eldest: Map.Entry<String?, Boolean?>): Boolean {
                return size > MAX_HIDDEN_CARDS
            }
        })
    var age = 0
    val cards = mutableListOf<Card>()

    init {
        updateHiddenCards()
    }

    fun updateHiddenCards() {
        hiddenCards.clear()
        hiddenCards.addAll(Prefs.getHiddenCards())
    }

    fun setFeedUpdateListener(listener: FeedUpdateListener?) {
        updateListener = listener
    }

    open fun reset() {
        wiki = null
        age = 0
        currentDayCardAge = -1
        for (client in pendingClients) {
            client.cancel()
        }
        pendingClients.clear()
        cards.clear()
    }

    fun incrementAge() {
        age++
    }

    fun more(wiki: WikiSite) {
        this.wiki = wiki
        if (cards.size == 0) {
            requestProgressCard()
        }
        if (DeviceUtil.isAccessibilityEnabled) {
            removeAccessibilityCard()
        }
        buildScript(age)
        requestCard(wiki)
    }

    fun finished(): Boolean {
        return pendingClients.isEmpty()
    }

    fun dismissCard(card: Card): Int {
        val position = cards.indexOf(card)
        when {
            card.type() === CardType.RANDOM -> {
                FeedContentType.RANDOM.isEnabled = false
                FeedContentType.saveState()
            }
            card.type() === CardType.MAIN_PAGE -> {
                FeedContentType.MAIN_PAGE.isEnabled = false
                FeedContentType.saveState()
            }
            else -> {
                addHiddenCard(card)
            }
        }
        removeCard(card, position)
        card.onDismiss()
        return position
    }

    fun undoDismissCard(card: Card, position: Int) {
        when {
            card.type() === CardType.RANDOM -> {
                FeedContentType.RANDOM.isEnabled = true
                FeedContentType.saveState()
            }
            card.type() === CardType.MAIN_PAGE -> {
                FeedContentType.MAIN_PAGE.isEnabled = true
                FeedContentType.saveState()
            }
            else -> unHideCard(card)
        }
        insertCard(card, position)
        card.onRestore()
    }

    protected abstract fun buildScript(age: Int)
    fun addPendingClient(client: FeedClient?) {
        if (client != null) {
            pendingClients.add(client)
        }
    }

    fun conditionallyAddPendingClient(client: FeedClient?, condition: Boolean) {
        if (condition && client != null) {
            pendingClients.add(client)
        }
    }

    // Call to kick off the request chain or to retry a failed request.  To move to the next pending
    // client, call requestNextCard.
    private fun requestCard(wiki: WikiSite) {
        if (pendingClients.isEmpty()) {
            removeProgressCard()
            return
        }
        pendingClients[0].request(context, wiki, age, callback)
    }

    private fun requestNextCard(wiki: WikiSite) {
        if (pendingClients.isNotEmpty()) {
            pendingClients.removeAt(0)
        }
        if (lastCard !is ProgressCard && shouldShowProgressCard(pendingClients[0])) {
            requestProgressCard()
        }
        requestCard(wiki)
    }

    fun requestOfflineCard() {
        if (lastCard !is OfflineCard) {
            appendCard(OfflineCard())
        }
    }

    fun removeOfflineCard() {
        if (lastCard is OfflineCard) {
            dismissCard(lastCard as OfflineCard)
        }
    }

    private val lastCard get() = if (cards.size > 1) cards[cards.size - 1] else null

    private fun requestProgressCard() {
        if (lastCard !is ProgressCard) {
            appendCard(progressCard)
        }
    }

    private fun removeProgressCard() {
        removeCard(progressCard, cards.indexOf(progressCard))
    }

    private fun setOfflineState() {
        removeProgressCard()
        appendCard(OfflineCard())
    }

    private fun removeAccessibilityCard() {
        if (lastCard is AccessibilityCard) {
            removeCard(lastCard as AccessibilityCard, cards.indexOf(lastCard))
            (lastCard as AccessibilityCard).onDismiss()
            // TODO: possible on optimization if automatically scroll up to the next card.
        }
    }

    private inner class ClientRequestCallback : FeedClient.Callback {
        override fun success(cardList: List<Card>) {
            var atLeastOneAppended = false
            for (card in cardList) {
                if (!isCardHidden(card)) {
                    appendCard(card)
                    atLeastOneAppended = true
                }
            }
            wiki?.let { requestNextCard(it) }
            if (pendingClients.isEmpty()) {
                updateListener?.finished(!atLeastOneAppended)
            }
        }

        override fun error(caught: Throwable) {
            if (ThrowableUtil.isOffline(caught)) {
                setOfflineState()
            } else {
                wiki?.let { requestNextCard(it) }
                L.w(caught)
            }
        }
    }

    private fun appendCard(card: Card) {
        val progressPos = cards.indexOf(progressCard)
        var pos = if (progressPos >= 0) progressPos else cards.size
        if (isDailyCardType(card) && currentDayCardAge < age) {
            currentDayCardAge = age
            insertCard(DayHeaderCard(currentDayCardAge), pos++)
        }
        insertCard(card, pos)
    }

    private fun insertCard(card: Card, position: Int) {
        if (position < 0) {
            return
        }
        cards.add(position, card)
        updateListener?.insert(card, position)
    }

    private fun removeCard(card: Card, position: Int) {
        if (position < 0) {
            return
        }
        cards.remove(card)
        updateListener?.remove(card, position)
    }

    private fun addHiddenCard(card: Card) {
        hiddenCards.add(card.hideKey)
        Prefs.setHiddenCards(hiddenCards)
    }

    private fun isCardHidden(card: Card): Boolean {
        return hiddenCards.contains(card.hideKey)
    }

    private fun unHideCard(card: Card) {
        hiddenCards.remove(card.hideKey)
        Prefs.setHiddenCards(hiddenCards)
    }

    private fun isDailyCardType(card: Card): Boolean {
        return card is NewsCard || card is OnThisDayCard ||
                card is MostReadListCard || card is FeaturedArticleCard ||
                card is FeaturedImageCard
    }

    private fun shouldShowProgressCard(pendingClient: FeedClient): Boolean {
        return pendingClient is SuggestedEditsFeedClient ||
                pendingClient is AnnouncementClient ||
                pendingClient is BecauseYouReadClient
    }

    companion object {
        private const val MAX_HIDDEN_CARDS = 100
    }
}
