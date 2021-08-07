package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import java.util.*

@JsonClass(generateAdapter = true)
class UserInfo(val name: String = "", val id: Int = 0, val groups: Set<String> = emptySet(),
               @Json(name = "editcount") val editCount: Int = 0,
               @Json(name = "latestcontrib") val latestContrib: Date = Date(0)) : BlockInfo()
