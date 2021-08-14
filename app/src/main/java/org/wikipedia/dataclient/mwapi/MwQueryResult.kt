package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.notifications.Notification
import org.wikipedia.notifications.Notification.SeenTime
import org.wikipedia.notifications.Notification.UnreadNotificationWikiItem
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfo
import org.wikipedia.util.DateUtil.iso8601DateParse
import java.util.*

class MwQueryResult : PostProcessable {

    val pages: MutableList<MwQueryPage>? = null
    private val redirects: MutableList<Redirect>? = null
    private val converted: MutableList<ConvertedTitle>? = null

    @SerializedName("userinfo")
    val userInfo: UserInfo? = null
    private val users: List<ListUserResponse>? = null
    private val tokens: Tokens? = null

    @SerializedName("authmanagerinfo")
    private val amInfo: MwAuthManagerInfo? = null
    private val echomarkread: MarkReadResponse? = null
    val echoMarkSeen: MarkReadResponse? = null
    private val notifications: NotificationList? = null
    private val unreadnotificationpages: Map<String, UnreadNotificationWikiItem>? = null

    @SerializedName("general")
    private val generalSiteInfo: SiteInfo? = null

    @SerializedName("wikimediaeditortaskscounts")
    private val editorTaskCounts: EditorTaskCounts? = null

    val watchlist: List<WatchlistItem> = emptyList()

    @SerializedName("usercontribs")
    private val userContributions: List<UserContribution>? = null

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

    fun notifications(): NotificationList? {
        return notifications
    }

    fun unreadNotificationWikis(): Map<String, UnreadNotificationWikiItem>? {
        return unreadnotificationpages
    }

    fun captchaId(): String? {
        var captchaId: String? = null
        if (amInfo != null) {
            for (request in amInfo.requests()) {
                if ("CaptchaAuthenticationRequest" == request.id()) {
                    captchaId = request.fields()["captchaId"]!!.value()
                }
            }
        }
        return captchaId
    }

    fun getUserResponse(userName: String): ListUserResponse? {
        if (users != null) {
            for (user in users) {
                // MediaWiki user names are case sensitive, but the first letter is always capitalized.
                if (StringUtils.capitalize(userName) == user.name()) {
                    return user
                }
            }
        }
        return null
    }

    fun langLinks(): MutableList<PageTitle> {
        val result: MutableList<PageTitle> = ArrayList()
        if (pages == null || pages.isEmpty() || pages[0].langLinks() == null) {
            return result
        }
        // noinspection ConstantConditions
        for (link in pages[0].langLinks()!!) {
            val title = PageTitle(link.title, forLanguageCode(link.lang))
            result.add(title)
        }
        return result
    }

    fun siteInfo(): SiteInfo? {
        return generalSiteInfo
    }

    fun editorTaskCounts(): EditorTaskCounts? {
        return editorTaskCounts
    }

    fun userContributions(): List<UserContribution> {
        return userContributions ?: emptyList()
    }

    val isEditProtected: Boolean
        get() {
            if (firstPage() == null || userInfo == null) {
                return false
            }
            for (protection in firstPage()!!.protection()) {
                if (protection.type == "edit" && !userInfo.groups.contains(protection.level)) {
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
        if (redirects == null || pages == null) {
            return
        }
        for (page in pages) {
            for (redirect in redirects) {
                // TODO: Looks like result pages and redirects can also be matched on the "index"
                // property.  Confirm in the API docs and consider updating.
                if (page.title() == redirect.to()) {
                    page.redirectFrom(redirect.from())
                    if (redirect.toFragment() != null) {
                        page.appendTitleFragment(redirect.toFragment())
                    }
                }
            }
        }
    }

    private fun resolveConvertedTitles() {
        if (converted == null || pages == null) {
            return
        }
        for (convertedTitle in converted) {
            for (page in pages) {
                if (page.title() == convertedTitle.to()) {
                    page.convertedFrom(convertedTitle.from())
                    page.convertedTo(convertedTitle.to())
                }
            }
        }
    }

    private class Redirect {
        private val index = 0
        private val from: String? = null
        private val to: String? = null

        @SerializedName("tofragment")
        private val toFragment: String? = null
        fun to(): String? {
            return to
        }

        fun from(): String? {
            return from
        }

        fun toFragment(): String? {
            return toFragment
        }
    }

    class ConvertedTitle {
        private val from: String? = null
        private val to: String? = null
        fun to(): String? {
            return to
        }

        fun from(): String? {
            return from
        }
    }

    private class Tokens {

        @SerializedName("csrftoken")
        val csrf: String? = null

        @SerializedName("createaccounttoken")
        val createAccount: String? = null

        @SerializedName("logintoken")
        val login: String? = null

        @SerializedName("watchtoken")
        val watch: String? = null
    }

    class MarkReadResponse {

        val result: String? = null
        val timestamp: String? = null
    }

    class NotificationList {

        val count = 0
        private val rawcount = 0
        val seenTime: SeenTime? = null
        val list: List<Notification>? = null

        @SerializedName("continue")
        val `continue`: String? = null
    }

    class WatchlistItem {
        private val pageid = 0
        val revid: Long = 0

        @SerializedName("old_revid")
        private val oldRevid: Long = 0
        val ns = 0
        val title: String = ""
        val user: String = ""
        private val timestamp: String? = null
        private val comment: String? = null
        private val parsedcomment: String? = null
        val logtype: String = ""
        val isAnon = false
        private val bot = false

        @SerializedName("new")
        private val isNew = false
        private val minor = false
        val oldlen = 0
        val newlen = 0
        var wiki: WikiSite? = null
        val date: Date
            get() = iso8601DateParse(StringUtils.defaultString(timestamp))
        val parsedComment: String = ""
    }
}
