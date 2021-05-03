package org.wikipedia.analytics

import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.CardType

class FeedFunnel(app: WikipediaApp) : TimedFunnel(app, SCHEMA_NAME, REVISION, SAMPLE_LOG_100) {

    private var entered = false

    fun enter() {
        if (!entered) {
            entered = true
            resetDuration()
            log(
                    "action", "enter"
            )
        }
    }

    fun exit() {
        if (entered) {
            entered = false
            log(
                    "action", "exit"
            )
        }
    }

    fun cardShown(cardType: CardType, languageCode: String?) {
        if (EXCLUDED_CARDS.contains(cardType)) {
            return
        }
        log(
                "action", "cardShown",
                "cardType", cardType.code(),
                "language", languageCode
        )
    }

    fun cardClicked(cardType: CardType, languageCode: String?) {
        if (EXCLUDED_CARDS.contains(cardType)) {
            return
        }
        log(
                "action", "cardClicked",
                "cardType", cardType.code(),
                "language", languageCode
        )
    }

    fun requestMore(age: Int) {
        log(
                "action", "more",
                "age", age
        )
    }

    fun refresh(age: Int) {
        log(
                "action", "refresh",
                "age", age
        )
    }

    fun dismissCard(cardType: CardType, position: Int) {
        log(
                "action", "dismiss",
                "cardType", cardType.code(),
                "position", position
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppFeed"
        private const val REVISION = 18115458
        private val EXCLUDED_CARDS = mutableListOf(CardType.SEARCH_BAR, CardType.PROGRESS)
    }
}
