package org.wikipedia.page.references

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PageReferences(val selectedIndex: Int = 0, val tid: String? = null,
                          val referencesGroup: List<Reference> = emptyList()) {
    @JsonClass(generateAdapter = true)
    data class Reference(val id: String? = null, val href: String? = null, val text: String = "",
                         @Json(name = "html") val content: String = "")
}
