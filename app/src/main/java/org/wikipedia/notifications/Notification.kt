package org.wikipedia.notifications

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.util.UriUtil.decodeURL
import java.util.*

@JsonClass(generateAdapter = true)
class Notification(
    val wiki: String = "",
    val id: Long = 0,
    val type: String = "",
    val category: String = "",
    @Json(name = "revid") val revID: Long = 0,
    val title: Title? = null,
    val agent: Agent? = null,
    internal val timestamp: Timestamp? = null,
    @Json(name = "*") val contents: Contents? = null,
    val sources: Map<String, Source> = emptyMap()
) {
    val key: Long
        get() = id + wiki.hashCode()
    val timestampValue: Date
        get() = timestamp?.utcIso8601 ?: Date()
    val isFromWikidata: Boolean
        get() = wiki == "wikidatawiki"

    override fun toString(): String {
        return id.toString()
    }

    @JsonClass(generateAdapter = true)
    class Title(
        var full: String = "",
        val text: String = "",
        internal val namespace: String = "",
        @Json(name = "namespace-key") internal val namespaceKey: Int = 0
    ) {
        val isMainNamespace: Boolean
            get() = namespaceKey == 0
    }

    @JsonClass(generateAdapter = true)
    class Agent(internal val id: Int = 0, val name: String = "")

    @JsonClass(generateAdapter = true)
    class Timestamp(var utcIso8601: Date = Date())

    @JsonClass(generateAdapter = true)
    class Link(val url: String = "", val label: String = "", val tooltip: String = "",
               internal val description: String? = null, val icon: String = "") {
        val decodedUrl: String
            get() = decodeURL(url)
    }

    @JsonClass(generateAdapter = true)
    class Links(val primary: Link? = null, val secondary: List<Link> = emptyList())

    @JsonClass(generateAdapter = true)
    class Source(val title: String = "", val url: String = "", internal val base: String = "") {
        val decodedUrl: String
            get() = decodeURL(url)
    }

    @JsonClass(generateAdapter = true)
    class Contents(
        val header: String = "",
        val compactHeader: String = "",
        val body: String = "",
        internal val icon: String = "",
        internal val iconUrl: String = "",
        val links: Links? = null
    ) {
        val decodedIconUrl: String
            get() = decodeURL(iconUrl)
    }

    @JsonClass(generateAdapter = true)
    class UnreadNotificationWikiItem(val totalCount: Int = 0, val source: Source? = null)

    @JsonClass(generateAdapter = true)
    class SeenTime(val alert: String? = null, val message: String? = null)

    companion object {
        const val CATEGORY_SYSTEM = "system"
        const val CATEGORY_SYSTEM_NO_EMAIL = "system-noemail" // default welcome
        const val CATEGORY_MILESTONE_EDIT = "thank-you-edit" // milestone
        const val CATEGORY_EDIT_USER_TALK = "edit-user-talk"
        const val CATEGORY_EDIT_THANK = "edit-thank"
        const val CATEGORY_REVERTED = "reverted"
        const val CATEGORY_LOGIN_FAIL = "login-fail"
        const val CATEGORY_MENTION =
            "mention" // combines "mention", "mention-failure" and "mention-success"
    }
}
