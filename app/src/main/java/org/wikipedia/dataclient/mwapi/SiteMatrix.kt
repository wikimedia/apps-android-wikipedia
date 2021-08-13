package org.wikipedia.dataclient.mwapi

import com.google.gson.JsonObject
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.wikipedia.json.GsonUtil
import java.util.*

@Serializable
class SiteMatrix : MwResponse() {

    val sitematrix: @Contextual JsonObject? = null

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
            siteMatrix.sitematrix?.keySet()?.filterNot { it == "count" }?.forEach { key ->
                GsonUtil.getDefaultGson().fromJson(
                    siteMatrix.sitematrix[key], SiteInfo::class.java
                )?.let {
                    sites.add(it)
                }
            }
            return sites
        }
    }
}
