package org.wikipedia.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UriUtil
import java.util.*

@Serializable
class Notification {

    @SerialName("*") val contents: Contents? = null
    private val timestamp: Timestamp? = null
    val category = ""
    val wiki = ""
    val id: Long = 0
    val type = ""
    val revid: Long = 0
    val title: Title? = null
    val agent: Agent? = null
    val sources: Map<String, Source>? = null
    val utcIso8601: String
        get() = timestamp?.utciso8601.orEmpty()
    val isFromWikidata: Boolean
        get() = wiki == "wikidatawiki"

    fun key(): Long {
        return id + wiki.hashCode()
    }

    fun getTimestamp(): Date {
        return timestamp?.date() ?: Date()
    }

    override fun toString(): String {
        return id.toString()
    }

    @Serializable
    class Title {

        @SerialName("namespace-key") private val namespaceKey = 0
        var full: String = ""
        val text: String = ""
        private val namespace: String? = null

        val isMainNamespace: Boolean
            get() = namespaceKey == 0
    }

    @Serializable
    class Agent {

        private val id = 0
        val name: String = ""
    }

    @Serializable
    class Timestamp {

        val utciso8601: String? = null

        fun date(): Date {
            return DateUtil.iso8601DateParse(utciso8601!!)
        }
    }

    @Serializable
    class Link {

        private val description: String? = null
        val url: String = ""
            get() = UriUtil.decodeURL(field)
        val label: String = ""
        val tooltip: String = ""
        val icon: String = ""
    }

    @Serializable
    class Links {

        private var primaryLink: Link? = null
       val primary: JsonElement? = null
        val secondary: List<Link>? = null

        fun getPrimary(): Link? {
            if (primary == null) {
                return null
            }
            if (primaryLink == null && primary is JsonObject) {
                primaryLink = Json.decodeFromJsonElement<Link>(buildJsonObject { primary })
            }
            return primaryLink
        }
    }

    @Serializable
    class Source {

        val title: String = ""
        val url: String = ""
            get() = UriUtil.decodeURL(field)
        val base: String = ""
    }

    @Serializable
    class Contents {

        private val icon: String? = null
        val header: String = ""
        val compactHeader: String = ""
        val body: String = ""
        val iconUrl: String = ""
            get() = UriUtil.decodeURL(field)
        val links: Links? = null
    }

    @Serializable
    class UnreadNotificationWikiItem {

        val totalCount = 0
        val source: Source? = null
    }

    @Serializable
    class SeenTime {

        val alert: String? = null
        val message: String? = null
    }

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
