package org.wikipedia.page

import org.wikipedia.Constants
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.UriUtil
import java.time.LocalDateTime

class Page(val title: PageTitle, var sections: List<Section> = emptyList(), val summary: PageSummary) {
    val isMainPage get() = summary.type == PageSummary.TYPE_MAIN_PAGE
    val isArticle get() = !isMainPage && summary.ns.main()
    val leadImageUrl get() = summary.thumbnailUrl?.let { ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE) }
    val leadImageName get() = UriUtil.decodeURL(summary.leadImageName.orEmpty())
    val leadImageWidth get() = summary.thumbnail?.width ?: 0
    val leadImageHeight get() = summary.thumbnail?.height ?: 0
    val lastModified get() = if (summary.timestamp.isEmpty()) LocalDateTime.now() else DateUtil.iso8601LocalDateTimeParse(summary.timestamp)

    var protection: Protection? = null
        set(value) {
            field = value
            val editProtectionStatus = value?.firstAllowedEditorRole.orEmpty()
            val isLoggedInUserAllowedToEdit = value?.run { AccountUtil.isMemberOf(editRoles) } == true
            canEdit = editProtectionStatus.isEmpty() || isLoggedInUserAllowedToEdit
        }

    // FIXME: This is not a true page property, since it depends on current user.
    var canEdit: Boolean = false
}
