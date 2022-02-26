package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.json.JsonUtil

@Serializable
class SiteMatrix : MwResponse() {

    val sitematrix: JsonObject? = null

    @Serializable
    class SiteInfo {

        val code: String = ""
        val name: String = ""
        val localname: String = ""
    }

    companion object {
        fun getSites(siteMatrix: SiteMatrix): List<SiteInfo> {
            // We have to parse the Json manually because the list of SiteInfo objects
            // contains a "count" member that prevents it from being able to deserialize
            // as a list automatically.
            return siteMatrix.sitematrix?.mapNotNull { (key, value) ->
                if (key != "count") {
                    JsonUtil.json.decodeFromJsonElement<SiteInfo>(value)
                } else {
                    null
                }
            }.orEmpty()
        }
    }
}
