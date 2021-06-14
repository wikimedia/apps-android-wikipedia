package org.wikipedia.readinglist.sync

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.annotations.Required
import org.wikipedia.util.DateUtil
import java.text.Normalizer
import java.util.*

@Serializable
data class SyncedReadingLists(val lists: List<RemoteReadingList>?,
                              val entries: List<RemoteReadingListEntry>?,
                              @SerializedName("next") val continueStr: String?) {

    constructor(lists: List<RemoteReadingList>?, entries: List<RemoteReadingListEntry>?) : this(lists, entries, null)

    @Serializable
    data class RemoteReadingList(@Required val id: Long,
                                 @SerializedName("default") val isDefault: Boolean,
                                 @Required private val name: String,
                                 private val description: String?,
                                 @Required val created: String,
                                 @Required val updated: String,
                                 @SerializedName("deleted") val isDeleted: Boolean) {

        constructor(name: String, description: String?) :
                this(0, false, name, description, DateUtil.iso8601DateFormat(Date()), DateUtil.iso8601DateFormat(Date()), false)

        fun name(): String = Normalizer.normalize(name, Normalizer.Form.NFC)
        fun description(): String? = Normalizer.normalize(description.orEmpty(), Normalizer.Form.NFC)
    }

    @Serializable
    data class RemoteReadingListEntry(val id: Long,
                                      val listId: Long,
                                      @Required private val project: String,
                                      @Required private val title: String,
                                      @Required val created: String,
                                      @Required val updated: String,
                                      val summary: PageSummary?,
                                      @SerializedName("deleted") val isDeleted: Boolean) {

        constructor(project: String, title: String) :
                this(0, 0, project, title, DateUtil.iso8601DateFormat(Date()), DateUtil.iso8601DateFormat(Date()), null, false)

        fun project(): String = Normalizer.normalize(project, Normalizer.Form.NFC)
        fun title(): String = Normalizer.normalize(title, Normalizer.Form.NFC)
    }

    @Serializable
    data class RemoteReadingListEntryBatch(val entries: List<RemoteReadingListEntry>) {
        val batch: Array<RemoteReadingListEntry> = entries.toTypedArray()
    }

    @Serializable
    inner class RemoteIdResponse {
        @Required val id: Long = 0
    }

    @Serializable
    inner class RemoteIdResponseBatch {
        @Required val batch: Array<RemoteIdResponse> = arrayOf()
    }
}
