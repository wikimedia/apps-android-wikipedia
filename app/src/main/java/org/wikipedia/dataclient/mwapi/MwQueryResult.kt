package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.Notification.SeenTime
import org.wikipedia.notifications.Notification.UnreadNotificationWikiItem
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfo
import org.wikipedia.util.DateUtil
import java.util.*

class MwQueryResult : PostProcessable {

    @SerializedName("userinfo") val userInfo: UserInfo? = null
    @SerializedName("unreadnotificationpages") val unreadNotificationWikis: Map<String, UnreadNotificationWikiItem>? = null
    @SerializedName("authmanagerinfo") private val amInfo: MwAuthManagerInfo? = null
    @SerializedName("general") val siteInfo: SiteInfo? = null
    @SerializedName("wikimediaeditortaskscounts") val editorTaskCounts: EditorTaskCounts? = null
    @SerializedName("usercontribs") val userContributions: List<UserContribution> = emptyList()

    private val redirects: MutableList<Redirect>? = null
    private val converted: MutableList<ConvertedTitle>? = null
    private val users: List<ListUserResponse>? = null
    private val tokens: Tokens? = null
    private val echomarkread: MarkReadResponse? = null
    val pages: MutableList<MwQueryPage>? = null
    val echoMarkSeen: MarkReadResponse? = null
    val notifications: NotificationList? = null
    val watchlist: List<WatchlistItem> = emptyList()

    fun firstPage(): MwQueryPage? {
        return if (pages != null && pages.size > 0) {
            pages[0]
        } else null
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
        return users?.find { StringUtils.capitalize(userName) == it.name }
    }

    fun langLinks(): MutableList<PageTitle> {
        val result = mutableListOf<PageTitle>()
        if (pages.isNullOrEmpty() || pages[0].langlinks == null) {
            return result
        }
        // noinspection ConstantConditions
        for (link in pages[0].langlinks) {
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

    override fun postProcess() {
        resolveConvertedTitles()
        resolveRedirectedTitles()
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

    private class Redirect(@SerializedName("tofragment") val toFragment: String? = null,
                           private val index: Int = 0,
                           val from: String? = null,
                           val to: String? = null)

    class ConvertedTitle(val from: String? = null, val to: String? = null)

    private class Tokens(@SerializedName("csrftoken") val csrf: String? = null,
                         @SerializedName("createaccounttoken") val createAccount: String? = null,
                         @SerializedName("logintoken") val login: String? = null,
                         @SerializedName("watchtoken") val watch: String? = null)

    class MarkReadResponse(val timestamp: String? = null, val result: String? = null)

    class NotificationList(val list: List<Notification>? = null,
                           val seenTime: SeenTime? = null,
                           val count: Int = 0,
                           private val rawcount: Int = 0,
                           @SerializedName("continue") val continueStr: String? = null)

    class WatchlistItem {

        @SerializedName("new") private val isNew = false
        @SerializedName("old_revid") private val oldRevid: Long = 0
        private val pageid = 0
        private val timestamp: String? = null
        private val comment: String? = null
        private val parsedcomment: String? = null
        private val minor = false
        private val bot = false
        val revid: Long = 0
        val ns = 0
        val title: String = ""
        val user: String = ""
        val logtype: String = ""
        val isAnon = false
        val oldlen = 0
        val newlen = 0
        var wiki: WikiSite? = null
        val parsedComment: String = ""
        val date: Date
            get() = DateUtil.iso8601DateParse(timestamp.orEmpty())
    }
}
