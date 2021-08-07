package org.wikipedia.dataclient.page

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TalkPage(val revision: Long = 0, val topics: List<Topic> = emptyList()) {
    @JsonClass(generateAdapter = true)
    class Topic(val id: Int = 0, val depth: Int = 0, val html: String = "", val shas: TopicShas = TopicShas(),
                val replies: List<TopicReply> = emptyList()) {
        val indicatorSha: String
            get() = shas.indicator
    }

    @JsonClass(generateAdapter = true)
    class TopicShas(val html: String = "", val indicator: String = "")

    @JsonClass(generateAdapter = true)
    class TopicReply(val depth: Int = 0, val sha: String = "", val html: String = "")
}
