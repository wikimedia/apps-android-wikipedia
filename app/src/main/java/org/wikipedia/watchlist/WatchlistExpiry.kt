package org.wikipedia.watchlist

enum class WatchlistExpiry(val expiry: String) {
    NEVER("never"),
    ONE_WEEK("1 week"),
    ONE_MONTH("1 month"),
    THREE_MONTH("3 months"),
    SIX_MONTH("6 months"),
}