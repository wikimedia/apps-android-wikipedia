package org.wikipedia.notifications

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.wikipedia.json.GsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UriUtil
import java.util.*

class Notification {

    @SerializedName("*")
    val contents: Contents? = null
    private val timestamp: Timestamp? = null
    private val read: String? = null
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

    var isUnread: Boolean = true
        get() = field && read.isNullOrEmpty()

    fun key(): Long {
        return id + wiki.hashCode()
    }

    fun getTimestamp(): Date {
        return timestamp?.date() ?: Date()
    }

    override fun toString(): String {
        return id.toString()
    }

    class Title {

        @SerializedName("namespace-key")
        private val namespaceKey = 0
        var full: String = ""
        val text: String = ""
        private val namespace: String = ""

        val isMainNamespace: Boolean
            get() = namespaceKey == Namespace.MAIN.code()
    }

    class Agent {

        private val id = 0
        val name: String = ""
    }

    class Timestamp {

        val utciso8601: String? = null

        fun date(): Date {
            return DateUtil.iso8601DateParse(utciso8601!!)
        }
    }

    class Link {

        private val description: String = ""
        val url: String = ""
            get() = UriUtil.decodeURL(field)
        val label: String = ""
        val tooltip: String = ""
        val icon: String = ""
    }

    class Links {

        private var primaryLink: Link? = null
        val primary: JsonElement? = null
        val secondary: List<Link>? = null

        fun getPrimary(): Link? {
            if (primary == null) {
                return null
            }
            if (primaryLink == null && primary is JsonObject) {
                primaryLink = GsonUtil.getDefaultGson().fromJson(primary, Link::class.java)
            }
            return primaryLink
        }
    }

    class Source {

        val title: String = ""
        val url: String = ""
            get() = UriUtil.decodeURL(field)
        val base: String = ""
    }

    class Contents {

        private val icon: String = ""
        val header: String = ""
        val compactHeader: String = ""
        val body: String = ""
        val iconUrl: String = ""
            get() = UriUtil.decodeURL(field)
        val links: Links? = null
    }

    class UnreadNotificationWikiItem {

        val totalCount = 0
        val source: Source? = null
    }

    class SeenTime {

        val alert: String = ""
        val message: String = ""
    }
}
