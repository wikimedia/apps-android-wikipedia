package org.wikipedia.readinglist.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.Normalizer

@Serializable
data class SyncedReadingLists(val lists: List<RemoteReadingList>? = null,
                              val entries: List<RemoteReadingListEntry>? = null,
                              @SerialName("next") val continueStr: String? = null) {

    @Serializable
    data class RemoteReadingList(
        val id: Long = 0,
        @SerialName("default") val isDefault: Boolean = false,
        private val name: String,
        private val description: String? = null,
        @SerialName("deleted") val isDeleted: Boolean = false
    ) {
        fun name(): String = Normalizer.normalize(name, Normalizer.Form.NFC)
        fun description(): String? = Normalizer.normalize(description.orEmpty(), Normalizer.Form.NFC)
    }

    @Serializable
    data class RemoteReadingListEntry(
        val id: Long = -1,
        val listId: Long = -1,
        private val project: String,
        private val title: String,
        @SerialName("deleted") val isDeleted: Boolean = false
    ) {
        fun project(): String = Normalizer.normalize(project, Normalizer.Form.NFC)
        fun title(): String = Normalizer.normalize(title, Normalizer.Form.NFC)
    }

    @Serializable
    data class RemoteReadingListEntryBatch(val batch: List<RemoteReadingListEntry>)

    @Serializable
    class RemoteIdResponse {
        val id: Long = 0
    }

    @Serializable
    class RemoteIdResponseBatch {
        val batch: Array<RemoteIdResponse> = arrayOf()
    }
}
