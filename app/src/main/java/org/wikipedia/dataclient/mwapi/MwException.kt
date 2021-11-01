package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

@Serializable
class MwException(val error: MwServiceError) : RuntimeException() {

    val title: String
        get() = error.title

    override val message: String
        get() = error.details
}
