package org.wikipedia.page

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.parcel.DateParceler
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.UriUtil
import java.util.Date

@Parcelize
@TypeParceler<Date, DateParceler>()
data class PageProperties(
    var pageId: Int = 0,
    var namespace: Namespace,
    var revisionId: Long = 0,
    var lastModified: Date = Date(),
    var displayTitle: String = "",
    private var editProtectionStatus: String = "",
    var isMainPage: Boolean = false,
    /** Nullable URL with no scheme. For example, foo.bar.com/ instead of http://foo.bar.com/.  */
    var leadImageUrl: String? = null,
    var leadImageName: String? = null,
    var leadImageWidth: Int = 0,
    var leadImageHeight: Int = 0,
    val geo: Location? = null,
    var wikiBaseItem: String? = null,
    var descriptionSource: String? = null,
    // FIXME: This is not a true page property, since it depends on current user.
    var canEdit: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    var protection: Protection? = null
        set(value) {
            field = value
            editProtectionStatus = value?.firstAllowedEditorRole.orEmpty()
            canEdit = editProtectionStatus.isEmpty() || isLoggedInUserAllowedToEdit
        }

    /**
     * Side note: Should later be moved out of this class but I like the similarities with
     * PageProperties(JSONObject).
     */
    constructor(pageSummary: PageSummary) : this(
        pageSummary.pageId,
        pageSummary.ns,
        pageSummary.revision,
        if (pageSummary.timestamp.isEmpty()) Date() else DateUtil.iso8601DateParse(pageSummary.timestamp),
        pageSummary.displayTitle,
        isMainPage = pageSummary.type == PageSummary.TYPE_MAIN_PAGE,
        leadImageUrl = pageSummary.thumbnailUrl?.let { ImageUrlUtil.getUrlForPreferredSize(it, DimenUtil.calculateLeadImageWidth()) },
        leadImageName = UriUtil.decodeURL(pageSummary.leadImageName.orEmpty()),
        leadImageWidth = pageSummary.thumbnailWidth,
        leadImageHeight = pageSummary.thumbnailHeight,
        geo = pageSummary.coordinates,
        wikiBaseItem = pageSummary.wikiBaseItem,
        descriptionSource = pageSummary.descriptionSource
    )

    private val isLoggedInUserAllowedToEdit: Boolean
        get() = protection?.run { AccountUtil.isMemberOf(editRoles) } ?: false
}
