package org.wikipedia.dataclient.mwapi

import android.net.Uri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.Notification.SeenTime
import org.wikipedia.notifications.Notification.UnreadNotificationWikiItem
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfo
import java.util.*

@JsonClass(generateAdapter = true)
class MwQueryResult(
    val pages: List<MwQueryPage> = emptyList(),
    internal val redirects: List<Redirect> = emptyList(),
    internal val converted: List<ConvertedTitle> = emptyList(),
    @Json(name = "userinfo") val userInfo: UserInfo = UserInfo(),
    internal val users: List<ListUserResponse> = emptyList(),
    internal val tokens: Tokens? = null,
    @Json(name = "authmanagerinfo") internal val amInfo: MwAuthManagerInfo? = null,
    @Json(name = "echomarkread") internal val echoMarkRead: MarkReadResponse? = null,
    @Json(name = "echomarkseen") val echoMarkSeen: MarkReadResponse? = null,
    val notifications: NotificationList? = null,
    val unreadNotificationPages: Map<String, UnreadNotificationWikiItem> = emptyMap(),
    @Json(name = "general") val siteInfo: SiteInfo? = null,
    @Json(name = "wikimediaeditortaskscounts") val editorTaskCounts: EditorTaskCounts? = null,
    val watchlist: List<WatchlistItem> = emptyList(),
    @Json(name = "usercontribs") val userContributions: List<UserContribution> = emptyList()
) {
    val firstPage: MwQueryPage?
        get() = pages.firstOrNull()
    val csrfToken: String?
        get() = tokens?.csrf
    val watchToken: String?
        get() = tokens?.watch
    val createAccountToken: String?
        get() = tokens?.createAccount
    val loginToken: String?
        get() = tokens?.login

    val captchaId: String?
        get() = amInfo?.requests?.filter { "CaptchaAuthenticationRequest" == it.id }
            ?.map { it.fields["captchaId"]!!.value }?.firstOrNull()

    // noinspection ConstantConditions
    val langLinks: List<PageTitle>
        get() = firstPage?.langLinks?.map {
            PageTitle(it.title, WikiSite.forLanguageCode(it.lang))
        } ?: emptyList()

    val isEditProtected: Boolean
        get() = firstPage?.protection?.any { it.type == "edit" && userInfo.groups.contains(it.level) } ?: false

    init {
        resolveConvertedTitles()
        resolveRedirectedTitles()
    }

    fun getUserResponse(userName: String): ListUserResponse? {
        return users.firstOrNull { user ->
            user.name == userName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    private fun resolveRedirectedTitles() {
        for (page in pages) {
            for (redirect in redirects) {
                // TODO: Looks like result pages and redirects can also be matched on the "index"
                // property.  Confirm in the API docs and consider updating.
                if (page.title == redirect.to) {
                    page.redirectFrom = redirect.from
                    if (redirect.toFragment != null) {
                        page.appendTitleFragment(redirect.toFragment)
                    }
                }
            }
        }
    }

    private fun resolveConvertedTitles() {
        for (convertedTitle in converted) {
            for (page in pages) {
                if (page.title == convertedTitle.to) {
                    page.convertedFrom = convertedTitle.from
                    page.convertedTo = convertedTitle.to
                }
            }
        }
    }

    @JsonClass(generateAdapter = true)
    class Redirect(internal val index: Int = 0, val from: String? = null, val to: String? = null,
                   @Json(name = "tofragment") val toFragment: String? = null)

    @JsonClass(generateAdapter = true)
    class ConvertedTitle(val from: String? = null, val to: String? = null)

    @JsonClass(generateAdapter = true)
    class Tokens(
        @Json(name = "csrftoken") internal val csrf: String? = null,
        @Json(name = "createaccounttoken") internal val createAccount: String? = null,
        @Json(name = "logintoken") internal val login: String? = null,
        @Json(name = "watchtoken") internal val watch: String? = null
    )

    @JsonClass(generateAdapter = true)
    class MarkReadResponse(val result: String? = null, val timestamp: String? = null)

    @JsonClass(generateAdapter = true)
    class NotificationList(
        val count: Int = 0,
        internal val rawcount: Int = 0,
        val seenTime: SeenTime? = null,
        val list: List<Notification> = emptyList(),
        val `continue`: String? = null
    )

    @JsonClass(generateAdapter = true)
    class WatchlistItem(
        @Json(name = "pageid") internal val pageId: Int = 0,
        @Json(name = "revid") val revId: Long = 0,
        @Json(name = "old_revid") internal val oldRevId: Long = 0,
        val ns: Int = 0,
        val title: String = "",
        val user: String = "",
        val timestamp: Date = Date(0),
        internal val comment: String = "",
        @Json(name = "parsedcomment") val parsedComment: String = "",
        @Json(name = "logtype") val logType: String = "",
        @Json(name = "anon") val isAnon: Boolean = false,
        internal val bot: Boolean = false,
        @Json(name = "new") internal val isNew: Boolean = false,
        internal val minor: Boolean = false,
        val oldlen: Int = 0,
        val newlen: Int = 0,
        var wiki: WikiSite = WikiSite(Uri.EMPTY)
    )
}
