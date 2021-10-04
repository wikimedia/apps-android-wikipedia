package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

@Serializable
open class MwPostResponse : MwResponse() {
    val pageInfo: MwQueryPage? = null
    val options: String? = null
    val success = 0
}
