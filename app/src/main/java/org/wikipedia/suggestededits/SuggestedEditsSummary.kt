package org.wikipedia.suggestededits

import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.page.PageTitle

data class SuggestedEditsSummary(
        var title: String,
        var lang: String,
        var pageTitle: PageTitle,
        var normalizedTitle: String?,
        var displayTitle: String?,
        var description: String?,
        var thumbnailUrl: String?,
        var originalUrl: String?,
        var extractHtml: String?,
        var timestamp: String?,
        var user: String?,
        var metadata: ExtMetadata?
)
