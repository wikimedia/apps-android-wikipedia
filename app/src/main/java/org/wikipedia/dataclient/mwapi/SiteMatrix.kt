package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class SiteMatrix(errors: List<MwServiceError> = emptyList(), val sitematrix: Map<String, Any> = emptyMap()) : MwResponse(errors) {
    @JsonClass(generateAdapter = true)
    class SiteInfo(val code: String = "", val name: String = "", val localname: String = "")

    companion object {
        fun getSites(siteMatrix: SiteMatrix): List<SiteInfo> {
            // We have to parse the Json manually because the list of SiteInfo objects
            // contains a "count" member that prevents it from being able to deserialize
            // as a list automatically.
            return siteMatrix.sitematrix.values.filterIsInstance<SiteInfo>()
        }
    }
}
