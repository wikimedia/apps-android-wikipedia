package org.wikipedia.settings

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SiteInfo(@Json(name = "mainpage") val mainPage: String? = null,
                    @Json(name = "sitename") val siteName: String? = null,
                    val lang: String? = null,
                    @Json(name = "readinglists-config") val readingListsConfig: ReadingListsConfig? = null) {
    @JsonClass(generateAdapter = true)
    data class ReadingListsConfig(val maxListsPerUser: Int = 0, val maxEntriesPerList: Int = 0,
                                  val deletedRetentionDays: Int = 0)
}
