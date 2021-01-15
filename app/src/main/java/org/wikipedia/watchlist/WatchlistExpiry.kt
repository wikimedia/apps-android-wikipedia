package org.wikipedia.watchlist

import androidx.annotation.StringRes
import org.wikipedia.R

enum class WatchlistExpiry(val expiry: String, @StringRes val stringId: Int) {
    NEVER("never", R.string.watchlist_page_add_to_watchlist_snackbar_period_permanently),
    ONE_WEEK("1 week", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_one_week),
    ONE_MONTH("1 month", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_one_month),
    THREE_MONTH("3 months", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_three_months),
    SIX_MONTH("6 months", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_six_months),
}
