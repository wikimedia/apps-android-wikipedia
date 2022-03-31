package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.Notification.SeenTime
import org.wikipedia.notifications.db.Notification.UnreadNotificationWikiItem
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfo
import org.wikipedia.util.DateUtil
import java.util.*

@Serializable
class MwQueryResult {

    @SerialName("userinfo") val userInfo: UserInfo? = null
    @SerialName("unreadnotificationpages") val unreadNotificationWikis: Map<String, UnreadNotificationWikiItem>? = null
    @SerialName("authmanagerinfo") private val amInfo: MwAuthManagerInfo? = null
    @SerialName("general") val siteInfo: SiteInfo? = null
    @SerialName("wikimediaeditortaskscounts") val editorTaskCounts: EditorTaskCounts? = null
    @SerialName("usercontribs") val userContributions: List<UserContribution> = emptyList()
    @SerialName("allusers") val allUsers: List<User>? = null

    private val redirects: MutableList<Redirect>? = null
    private val converted: MutableList<ConvertedTitle>? = null
    private val users: List<ListUserResponse>? = null
    private val tokens: Tokens? = null
    private val echomarkread: MarkReadResponse? = null
    val pages: MutableList<MwQueryPage>? = null
    val echomarkseen: MarkReadResponse? = null
    val notifications: NotificationList? = null
    val watchlist: List<WatchlistItem> = emptyList()
    val namespaces: Map<String, Namespace>? = null
    val allmessages: List<Message>? = null

    init {
        resolveConvertedTitles()
        resolveRedirectedTitles()
    }

    fun firstPage(): MwQueryPage? {
        return pages?.firstOrNull()
    }

    fun csrfToken(): String? {
        return tokens?.csrf
    }

    fun watchToken(): String? {
        return tokens?.watch
    }

    fun createAccountToken(): String? {
        return tokens?.createAccount
    }

    fun loginToken(): String? {
        return tokens?.login
    }

    fun captchaId(): String? {
        return amInfo?.requests?.find { "CaptchaAuthenticationRequest" == it.id }?.fields?.get("captchaId")?.value
    }

    fun getUserResponse(userName: String): ListUserResponse? {
        // MediaWiki user names are case sensitive, but the first letter is always capitalized.
        return users?.find { userName.capitalize(Locale.getDefault()) == it.name }
    }

    fun langLinks(): MutableList<PageTitle> {
        val result = mutableListOf<PageTitle>()
        if (pages.isNullOrEmpty()) {
            return result
        }
        // noinspection ConstantConditions
        for (link in pages.first().langlinks) {
            val title = PageTitle(link.title, WikiSite.forLanguageCode(link.lang))
            result.add(title)
        }
        return result
    }

    val isEditProtected: Boolean
        get() {
            if (firstPage() == null || userInfo == null) {
                return false
            }
            for (protection in firstPage()!!.protection) {
                if (protection.type == "edit" && !userInfo.groups().contains(protection.level)) {
                    return true
                }
            }
            return false
        }

    private fun resolveRedirectedTitles() {
        if (redirects.isNullOrEmpty() || pages.isNullOrEmpty()) {
            return
        }
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
        if (converted.isNullOrEmpty() || pages.isNullOrEmpty()) {
            return
        }
        converted.forEach { convertedTitle ->
            pages.filter { it.title == convertedTitle.to }.forEach { page ->
                page.convertedFrom = convertedTitle.from
                page.convertedTo = convertedTitle.to
            }
        }
    }

    @Serializable
    private class Redirect(@SerialName("tofragment") val toFragment: String? = null,
                           private val index: Int = 0,
                           val from: String? = null,
                           val to: String? = null)

    @Serializable
    class ConvertedTitle(val from: String? = null, val to: String? = null)

    @Serializable
    private class Tokens(@SerialName("csrftoken") val csrf: String? = null,
                         @SerialName("createaccounttoken") val createAccount: String? = null,
                         @SerialName("logintoken") val login: String? = null,
                         @SerialName("watchtoken") val watch: String? = null)

    @Serializable
    class MarkReadResponse(val timestamp: String? = null, val result: String? = null)

    @Serializable
    class NotificationList(val list: List<Notification>? = null,
                           val seenTime: SeenTime? = null,
                           val count: Int = 0,
                           private val rawcount: Int = 0,
                           @SerialName("continue") val continueStr: String? = null)

    @Serializable
    class WatchlistItem {

        @SerialName("new") private val isNew = false
        @SerialName("anon") val isAnon = false
        @SerialName("old_revid") private val oldRevid: Long = 0
        private val pageid = 0
        private val timestamp: String? = null
        private val comment: String? = null
        private val minor = false
        private val bot = false
        val revid: Long = 0
        val ns = 0
        val title: String = ""
        val user: String = ""
        val logtype: String = ""
        val oldlen = 0
        val newlen = 0
        var wiki: WikiSite? = null
        @SerialName("parsedcomment") val parsedComment: String = ""
        val date: Date
            get() = DateUtil.iso8601DateParse(timestamp.orEmpty())
    }

    @Serializable
    class Namespace {
        val id: Int = 0
        val name: String = ""
    }

    @Serializable
    class User {
        val userid: Int = 0
        val name: String = ""
    }

    @Serializable
    class Message {
        val name: String = ""
        val content: String = ""
    }
}
