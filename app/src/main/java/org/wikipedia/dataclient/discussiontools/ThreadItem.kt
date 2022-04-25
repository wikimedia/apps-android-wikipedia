package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.Serializable

@Serializable
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
) {
    var isExpanded = false
        private set

    fun expand() {
        replies.forEach { it.expand() }
        isExpanded = true
    }

    fun collapse() {
        replies.forEach { it.collapse() }
        isExpanded = false
    }
}
