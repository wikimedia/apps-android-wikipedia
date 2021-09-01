package org.wikipedia.page

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.page.Protection
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.UriUtil
import java.util.*

/**
 * Immutable class that contains metadata associated with a PageTitle.
 */
@Parcelize
data class PageProperties constructor(
    val pageId: Int = 0,
    val namespace: Namespace,
    val revisionId: Long = 0,
    val lastModified: Date = Date(),
    val displayTitle: String = "",
    private var editProtectionStatus: String = "",
    val isMainPage: Boolean = false,
    /**
     * @return Nullable URL with no scheme. For example, foo.bar.com/ instead of
     * http://foo.bar.com/.
     */
    /** Nullable URL with no scheme. For example, foo.bar.com/ instead of http://foo.bar.com/.  */
    val leadImageUrl: String? = null,
    val leadImageName: String? = null,
    val leadImageWidth: Int = 0,
    val leadImageHeight: Int = 0,
    val geo: Location? = null,
    val wikiBaseItem: String? = null,
    val descriptionSource: String? = null,
    /**
     * True if the user who first requested this page can edit this page
     * FIXME: This is not a true page property, since it depends on current user.
     */
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
        geo = pageSummary.geo,
        wikiBaseItem = pageSummary.wikiBaseItem,
        descriptionSource = pageSummary.descriptionSource
    )

    /**
     * Constructor to be used when building a Page from a compilation. Initializes the title and
     * namespace fields, and explicitly disables editing. All other fields initialized to defaults.
     * @param title Title to which these properties apply.
     */
    constructor(title: PageTitle, isMainPage: Boolean) : this(namespace = title.namespace(),
        displayTitle = title.displayText, isMainPage = isMainPage)

    private val isLoggedInUserAllowedToEdit: Boolean
        get() = protection != null && AccountUtil.isMemberOf(protection!!.editRoles)
}
