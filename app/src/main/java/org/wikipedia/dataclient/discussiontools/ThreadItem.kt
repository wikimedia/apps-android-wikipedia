package org.wikipedia.dataclient.discussiontools

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
        val replies: List<ThreadItem> = emptyList()
) : Parcelable {
    @IgnoredOnParcel @Transient var isExpanded = false
    @IgnoredOnParcel @Transient var isLastSibling = false

    @IgnoredOnParcel val allReplies: List<ThreadItem>
        get() {
            val list = mutableListOf<ThreadItem>()
            replies.forEach {
                list.add(it)
                list.addAll(it.allReplies)
            }
            return list
        }
}
