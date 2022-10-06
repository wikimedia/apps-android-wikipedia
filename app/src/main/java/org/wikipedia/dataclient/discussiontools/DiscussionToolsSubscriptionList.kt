package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.Serializable

@Serializable
class DiscussionToolsSubscriptionList {
    val subscriptions: Map<String, Int> = emptyMap()
}
