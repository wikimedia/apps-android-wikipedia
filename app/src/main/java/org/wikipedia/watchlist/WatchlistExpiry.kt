package org.wikipedia.watchlist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wikipedia.R

enum class WatchlistExpiry(val expiry: String,
                           @StringRes val stringId: Int,
                           @DrawableRes val icon: Int? = null) {
    NEVER(
        "never",
        R.string.watchlist_page_add_to_watchlist_snackbar_period_permanently,
        R.drawable.ic_star_24
    ),
    ONE_WEEK(
        "1 week",
        R.string.watchlist_page_add_to_watchlist_snackbar_period_for_one_week,
        R.drawable.ic_baseline_star_half_24
    ),
    ONE_MONTH("1 month", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_one_month),
    THREE_MONTH("3 months", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_three_months),
    SIX_MONTH("6 months", R.string.watchlist_page_add_to_watchlist_snackbar_period_for_six_months),
}
