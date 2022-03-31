package org.wikipedia.readinglist.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.DateUtil
import java.text.Normalizer
import java.util.*

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
        val created: String = DateUtil.iso8601DateFormat(Date()),
        val updated: String = DateUtil.iso8601DateFormat(Date()),
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
        val created: String = DateUtil.iso8601DateFormat(Date()),
        val updated: String = DateUtil.iso8601DateFormat(Date()),
        val summary: PageSummary? = null,
        @SerialName("deleted") val isDeleted: Boolean = false
    ) {
        fun project(): String = Normalizer.normalize(project, Normalizer.Form.NFC)
        fun title(): String = Normalizer.normalize(title, Normalizer.Form.NFC)
    }

    @Serializable
    data class RemoteReadingListEntryBatch(val entries: List<RemoteReadingListEntry>) {
        val batch: Array<RemoteReadingListEntry> = entries.toTypedArray()
    }

    @Serializable
    class RemoteIdResponse {
        val id: Long = 0
    }

    @Serializable
    class RemoteIdResponseBatch {
        val batch: Array<RemoteIdResponse> = arrayOf()
    }
}
