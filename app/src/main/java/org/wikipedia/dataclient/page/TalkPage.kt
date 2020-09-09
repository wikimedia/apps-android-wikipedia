package org.wikipedia.dataclient.page

class TalkPage {
    val revision: Long = 0
    val topics: List<Topic>? = null
        get() = field ?: emptyList()

    class Topic {
        val id = 0
        val depth = 0
        val html: String? = null
            get() = field.orEmpty()
        val shas: TopicShas? = null
        val replies: List<TopicReply>? = null
            get() = field ?: emptyList()

        fun getIndicatorSha(): String {
            return shas?.indicator.orEmpty()
        }
    }

    class TopicShas {
        val html: String? = null
            get() = field.orEmpty()
        val indicator: String? = null
            get() = field.orEmpty()
    }

    class TopicReply {
        val depth = 0
        val sha: String? = null
            get() = field.orEmpty()
        val html: String? = null
            get() = field.orEmpty()
    }
}
