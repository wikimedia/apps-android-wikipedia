package org.wikipedia.feed.news

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.ImageUrlUtil

@Parcelize
@Serializable
class NewsItem(
    val story: String = "",
    val links: List<PageSummary> = emptyList()
) : Parcelable {
    fun thumb(): Uri? {
        return getFirstImageUri(links)?.let {
            ImageUrlUtil.getUrlForPreferredSize(
                it.toString(), Constants.PREFERRED_CARD_THUMBNAIL_SIZE
            ).toUri()
        }
    }

    fun thumbUrl(): String? {
        return getFirstImageUri(links)?.let {
            ImageUrlUtil.getUrlForPreferredSize(
                it.toString(), Constants.PREFERRED_CARD_THUMBNAIL_SIZE
            )
        }
    }

    private fun getFirstImageUri(links: List<PageSummary>): Uri? {
        return links.firstOrNull { !it.thumbnailUrl.isNullOrEmpty() }?.thumbnailUrl?.toUri()
    }
}
