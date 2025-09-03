package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
class PreviewRequest(
    val wikitext: String
)
