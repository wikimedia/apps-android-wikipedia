package org.wikipedia.dataclient.discussiontools

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil

@Serializable
@Parcelize
class ThreadItem(
        val type: String = "",
        val level: Int = 0,
        val id: String = "",
        val name: String = "",
        val html: String = "",
        val author: String = "",
        val timestamp: String = "",
        val headingLevel: Int = 0,
        val placeholderHeading: Boolean = false,
        val replies: List<ThreadItem> = emptyList(),
        val othercontent: String = ""
) : Parcelable {
    @IgnoredOnParcel @Transient var isExpanded = true
    @IgnoredOnParcel @Transient var isFirstTopLevel = false
    @IgnoredOnParcel @Transient var isLastSibling = false
    @IgnoredOnParcel @Transient var seen: Boolean = false
    @IgnoredOnParcel @Transient var subscribed: Boolean = false
    // Pre-convert plaintext versions of the html and othercontent fields, for more efficient searching.
    @IgnoredOnParcel @Transient val plainText = StringUtil.fromHtml(StringUtil.removeStyleTags(html)).toString()
    @IgnoredOnParcel @Transient val plainOtherContent = StringUtil.fromHtml(StringUtil.removeStyleTags(othercontent)).toString()

    @IgnoredOnParcel val allReplies: List<ThreadItem>
        get() {
            val list = mutableListOf<ThreadItem>()
            replies.forEach {
                list.add(it)
                list.addAll(it.allReplies)
            }
            return list
        }

    @IgnoredOnParcel @Transient val date = try {
        if (timestamp.isEmpty()) {
            null
        } else if (timestamp.contains("T")) {
            // Assume a ISO 8601 timestamp
            DateUtil.iso8601DateParse(timestamp)
        } else {
            // Assume a DB timestamp
            DateUtil.dbDateParse(timestamp)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
