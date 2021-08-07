package org.wikipedia.readinglist.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.annotations.Required
import java.text.Normalizer

@JsonClass(generateAdapter = true)
data class SyncedReadingLists(val lists: List<RemoteReadingList> = emptyList(),
                              val entries: List<RemoteReadingListEntry> = emptyList(),
                              @Json(name = "next") val continueStr: String? = null) {

    @JsonClass(generateAdapter = true)
    data class RemoteReadingList(@Required val id: Long,
                                 @Json(name = "default") val isDefault: Boolean = false,
                                 @Required internal val name: String,
                                 internal val description: String? = null,
                                 @Required val created: String,
                                 @Required val updated: String,
                                 @Json(name = "deleted") val isDeleted: Boolean = false) {
        fun name(): String = Normalizer.normalize(name, Normalizer.Form.NFC)
        fun description(): String? = Normalizer.normalize(description.orEmpty(), Normalizer.Form.NFC)
    }

    @JsonClass(generateAdapter = true)
    data class RemoteReadingListEntry(val id: Long = 0,
                                      val listId: Long = 0,
                                      @Required internal val project: String,
                                      @Required internal val title: String,
                                      @Required val created: String,
                                      @Required val updated: String,
                                      val summary: PageSummary? = null,
                                      @Json(name = "deleted") val isDeleted: Boolean = false) {
        fun project(): String = Normalizer.normalize(project, Normalizer.Form.NFC)
        fun title(): String = Normalizer.normalize(title, Normalizer.Form.NFC)
    }

    @JsonClass(generateAdapter = true)
    data class RemoteReadingListEntryBatch(val entries: List<RemoteReadingListEntry> = emptyList()) {
        val batch: Array<RemoteReadingListEntry> = entries.toTypedArray()
    }

    @JsonClass(generateAdapter = true)
    class RemoteIdResponse(val id: Long = 0)

    @JsonClass(generateAdapter = true)
    class RemoteIdResponseBatch(val batch: Array<RemoteIdResponse> = emptyArray())
}
