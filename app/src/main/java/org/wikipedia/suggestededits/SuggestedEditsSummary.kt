package org.wikipedia.suggestededits

import org.wikipedia.Constants
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil

data class SuggestedEditsSummary(
        var title: String,
        var lang: String,
        var pageTitle: PageTitle,
        var normalizedTitle: String?,
        var displayTitle: String?,
        var description: String?,
        var thumbnailUrl: String?,
        var extractHtml: String?,
        var timestamp: String?,
        var user: String?,
        var metadata: ExtMetadata?
) {
    fun getPreferredSizeThumbnailUrl(): String = ImageUrlUtil.getUrlForPreferredSize(thumbnailUrl!!, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)
}
