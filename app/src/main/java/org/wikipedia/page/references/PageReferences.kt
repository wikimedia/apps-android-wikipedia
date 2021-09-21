package org.wikipedia.page.references

import kotlinx.serialization.SerialName

data class PageReferences(val selectedIndex: Int = 0,
                          val tid: String?,
                          val referencesGroup: List<Reference>? = emptyList()) {

    data class Reference(val id: String?,
                         val href: String?,
                         val text: String = "",
                         @SerialName("html") val content: String = "")
}
