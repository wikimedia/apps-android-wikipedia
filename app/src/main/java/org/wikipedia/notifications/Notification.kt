package org.wikipedia.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UriUtil
import java.util.*

@Serializable
class Notification(@SerialName("*") val contents: Contents? = null,
                   private val timestamp: Timestamp? = null,
                   var read: String? = null,
                   val category: String = "",
                   val wiki: String = "",
                   val id: Long = 0,
                   val type: String = "",
                   val revid: Long = 0,
                   val title: Title? = null,
                   val agent: Agent? = null) {

    val utcIso8601: String
        get() = timestamp?.utciso8601.orEmpty()

    val isFromWikidata: Boolean
        get() = wiki == "wikidatawiki"

    val isUnread get() = read.isNullOrEmpty()

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

        @SerialName("namespace-key")
        private val namespaceKey = 0
        var full: String = ""
        val text: String = ""
        private val namespace: String? = null

        val isMainNamespace: Boolean
            get() = namespaceKey == Namespace.MAIN.code()
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

        private val description: String = ""
        val url: String = ""
            get() = UriUtil.decodeURL(field)
        val label: String = ""
        val tooltip: String = ""
        // The icon could be a string or `false`.
        private val icon: JsonElement? = null

        fun icon(): String {
            return if (icon?.jsonPrimitive?.isString == true) icon.jsonPrimitive.content else ""
        }
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
                primaryLink = JsonUtil.json.decodeFromJsonElement<Link>(primary)
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

        private val icon: String = ""
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

        val alert: String = ""
        val message: String = ""
    }
}
