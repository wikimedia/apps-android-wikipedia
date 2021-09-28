package org.wikipedia.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SiteInfo(val mainpage: String?, val sitename: String?, val lang: String?,
                    @SerialName("readinglists-config")
                    val readingListsConfig: ReadingListsConfig?) {

    @Serializable
    data class ReadingListsConfig(val maxListsPerUser: Int = 0,
                                  val maxEntriesPerList: Int = 0,
                                  val deletedRetentionDays: Int = 0)
}
