package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
class SiteMatrix : MwResponse() {

    val sitematrix: JsonObject? = null

    inner class SiteInfo {

        val code: String = ""
        val name: String = ""
        val localname: String = ""
    }

    companion object {

        fun getSites(siteMatrix: SiteMatrix): List<SiteInfo> {
            val sites = mutableListOf<SiteInfo>()
            // We have to parse the Json manually because the list of SiteInfo objects
            // contains a "count" member that prevents it from being able to deserialize
            // as a list automatically.
            siteMatrix.sitematrix?.keys?.filterNot { it == "count" }?.forEach { key ->
                Json.decodeFromJsonElement<SiteInfo>(buildJsonObject { siteMatrix.sitematrix[key] }).let { sites.add(it) }
            }
            return sites
        }
    }
}
