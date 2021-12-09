package org.wikipedia.notifications.db

import androidx.room.Entity
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
@Entity(primaryKeys = ["id", "wiki"])
class Notification(var id: Long = 0,
                   var wiki: String = "",
                   var read: String? = null,
                   var category: String = "",
                   var type: String = "",
                   var revid: Long = 0,
                   var title: Title? = null,
                   var agent: Agent? = null,
                   var timestamp: Timestamp? = null,
                   @SerialName("*") var contents: Contents? = null) {

    val utcIso8601: String
        get() = timestamp?.utciso8601.orEmpty()

    val isFromWikidata: Boolean
        get() = wiki == "wikidatawiki"

    val isUnread get() = read.isNullOrEmpty()

    fun key(): Long {
        return id + wiki.hashCode()
    }

    fun date(): Date {
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
        private val primary: JsonElement? = null
        private val legacyPrimary: JsonElement? = null
        val secondary: List<Link>? = null

        fun getPrimary(): Link? {
            if (primaryLink == null) {
                if (legacyPrimary != null && legacyPrimary is JsonObject) {
                    primaryLink = JsonUtil.json.decodeFromJsonElement<Link>(legacyPrimary)
                } else if (primary != null && primary is JsonObject) {
                    primaryLink = JsonUtil.json.decodeFromJsonElement<Link>(primary)
                }
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
