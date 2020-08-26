package org.wikipedia.dataclient.page

class TalkPage {
    private val revision: Long = 0
    private val topics: List<Topic>? = null
        get() = field ?: emptyList()

    class Topic {
        private val id = 0
        private val depth = 0
        private val html: String? = null
        private val shas: TopicShas? = null
        val replies: List<TopicReply>? = null
            get() = field ?: emptyList()
    }

    class TopicShas {
        private val html: String? = null
        private val indicator: String? = null
    }

    class TopicReply {
        private val depth = 0
        private val sha: String? = null
        private val html: String? = null
    }
}