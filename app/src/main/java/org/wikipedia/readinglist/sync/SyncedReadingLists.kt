package org.wikipedia.readinglist.sync

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.annotations.Required
import org.wikipedia.util.DateUtil
import java.text.Normalizer
import java.util.*

data class SyncedReadingLists(val lists: List<RemoteReadingList>?,
                              val entries: List<RemoteReadingListEntry>?,
                              @SerializedName("next") val continueStr: String? = null) {

    data class RemoteReadingList(@Required val id: Long,
                                 @SerializedName("default") val isDefault: Boolean,
                                 @SerializedName("name") private val _name: String,
                                 @SerializedName("description") private val _description: String?,
                                 @Required val created: String,
                                 @Required val updated: String,
                                 @SerializedName("deleted") val isDeleted: Boolean) {

        constructor(_name: String, _description: String?) :
                this(0, false, _name, _description, DateUtil.iso8601DateFormat(Date()), DateUtil.iso8601DateFormat(Date()), false)

        @Required val name: String = Normalizer.normalize(_name, Normalizer.Form.NFC)
        val description: String? = Normalizer.normalize(_description.orEmpty(), Normalizer.Form.NFC)
    }

    data class RemoteReadingListEntry(val id: Long,
                                      val listId: Long,
                                      @SerializedName("project") private val _project: String,
                                      @SerializedName("title") private val _title: String,
                                      @Required val created: String,
                                      @Required val updated: String,
                                      val summary: PageSummary?,
                                      @SerializedName("deleted") val isDeleted: Boolean) {

        constructor(_project: String, _title: String) :
                this(0, 0, _project, _title, DateUtil.iso8601DateFormat(Date()), DateUtil.iso8601DateFormat(Date()), null, false)

        @Required
        val project: String = Normalizer.normalize(_project, Normalizer.Form.NFC)

        @Required
        val title: String = Normalizer.normalize(_title, Normalizer.Form.NFC)
    }

    data class RemoteReadingListEntryBatch(val entries: List<RemoteReadingListEntry>) {
        val batch: Array<RemoteReadingListEntry> = entries.toTypedArray()
    }

    inner class RemoteIdResponse {
        @Required val id: Long = 0
    }

    inner class RemoteIdResponseBatch {
        @Required val batch: Array<RemoteIdResponse> = arrayOf()
    }
}
