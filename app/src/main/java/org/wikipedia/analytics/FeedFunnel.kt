package org.wikipedia.analytics

import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.CardType

class FeedFunnel(app: WikipediaApp) : TimedFunnel(app, SCHEMA_NAME, REVISION, SAMPLE_LOG_100) {

    private var entered = false

    fun enter() {
        if (!entered) {
            entered = true
            resetDuration()
            log("action" to "enter")
        }
    }

    fun exit() {
        if (entered) {
            entered = false
            log("action" to "exit")
        }
    }

    fun cardShown(cardType: CardType, languageCode: String?) {
        if (EXCLUDED_CARDS.contains(cardType)) {
            return
        }
        log("action" to "cardShown", "cardType" to cardType.code(), "language" to languageCode)
    }

    fun cardClicked(cardType: CardType, languageCode: String?) {
        if (EXCLUDED_CARDS.contains(cardType)) {
            return
        }
        log("action" to "cardClicked", "cardType" to cardType.code(), "language" to languageCode)
    }

    fun requestMore(age: Int) {
        log("action" to "more", "age" to age)
    }

    fun refresh(age: Int) {
        log("action" to "refresh", "age" to age)
    }

    fun dismissCard(cardType: CardType, position: Int) {
        log("action" to "dismiss", "cardType" to cardType.code(), "position" to position)
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFeed"
        private const val REVISION = 18115458
        private val EXCLUDED_CARDS = listOf(CardType.SEARCH_BAR, CardType.PROGRESS)
    }
}
