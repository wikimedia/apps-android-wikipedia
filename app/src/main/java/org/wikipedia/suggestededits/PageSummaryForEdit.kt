package org.wikipedia.suggestededits

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.Constants
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil

@Parcelize
data class PageSummaryForEdit(
        var title: String,
        var lang: String,
        var pageTitle: PageTitle,
        var displayTitle: String?,
        var description: String?,
        var thumbnailUrl: String?,
        var extract: String? = null,
        var extractHtml: String? = null,
        var timestamp: String? = null,
        var user: String? = null,
        var metadata: ExtMetadata? = null
) : Parcelable {
    constructor(title: String,
                lang: String,
                pageTitle: PageTitle,
                displayTitle: String?,
                description: String?,
                thumbnailUrl: String?
    ) : this(title, lang, pageTitle, displayTitle, description, thumbnailUrl, null, null)

    fun getPreferredSizeThumbnailUrl(): String = ImageUrlUtil.getUrlForPreferredSize(thumbnailUrl!!, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)
}
