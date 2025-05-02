package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent

fun YearInReviewShareSheet(context: Context) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "https://wikipedia.org")
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Link to Share")
    shareIntent.putExtra(Intent.EXTRA_TITLE, "Link to Share")
    context.startActivity(shareIntent)
}