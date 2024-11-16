package org.wikipedia.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SiteInfo(
    val lang: String? = null,
    @SerialName("readinglists-config") val readingListsConfig: ReadingListsConfig? = null
) {

    // TODO: remove this class when temporary accounts are rolled out to all wikis.
    @Serializable
    class AutoCreateTempUser(val enabled: Boolean = true)

    @Serializable
    class ReadingListsConfig(val maxListsPerUser: Int = 0,
                             val maxEntriesPerList: Int = 0,
                             val deletedRetentionDays: Int = 0)
}
