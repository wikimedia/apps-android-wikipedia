package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class EditCount {

    val count: Int = 0
    val limit: Boolean = false

    companion object {
        const val EDIT_TYPE_ANONYMOUS = "anonymous"
        const val EDIT_TYPE_BOT = "bot"
        const val EDIT_TYPE_EDITORS = "editors"
        const val EDIT_TYPE_EDITS = "edits"
        const val EDIT_TYPE_MINOR = "minor"
        const val EDIT_TYPE_REVERTED = "reverted"
    }
}
