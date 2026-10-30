package org.wikipedia.topics.db

data class TopicReadStats(
    val topic: String,
    val articleCount: Int,
    val timeSpentSec: Int
)
