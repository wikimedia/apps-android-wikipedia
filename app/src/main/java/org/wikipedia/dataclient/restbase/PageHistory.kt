package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class PageHistory {

    val revisions: List<Revision> = emptyList()
    val latest: String = ""
    val older: String? = null
    val newer: String? = null

    @Serializable
    class Revision {
        val id: Long = 0
        val timestamp: String = ""
        val minor: Boolean = false
        val size: Long = 0
        val comment: String = ""
        val delta: Long = 0
        val user: User? = null

        val isAnon get() = user?.id == null
    }

    @Serializable
    class User {
        val id: Long? = null
        val name: String = ""
    }
}
