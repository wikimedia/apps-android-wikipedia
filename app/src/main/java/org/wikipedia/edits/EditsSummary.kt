package org.wikipedia.edits

import org.wikipedia.Constants
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil

data class EditsSummary(
        var title: String,
        var lang: String,
        var pageTitle: PageTitle,
        var displayTitle: String?,
        var description: String?,
        var thumbnailUrl: String?,
        var extractHtml: String? = null,
        var timestamp: String? = null,
        var user: String? = null,
        var metadata: ExtMetadata? = null
) {
    constructor(title: String,
                lang: String,
                pageTitle: PageTitle,
                displayTitle: String?,
                description: String?,
                thumbnailUrl: String?
    ) : this(title, lang, pageTitle, displayTitle, description, thumbnailUrl, null)

    fun getPreferredSizeThumbnailUrl(): String = ImageUrlUtil.getUrlForPreferredSize(thumbnailUrl!!, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)
}
