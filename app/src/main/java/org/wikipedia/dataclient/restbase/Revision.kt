package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class Revision {
    val id = 0L
    val size = 0
    val timestamp = ""
    val delta: Int? = null
    val source = ""
}
