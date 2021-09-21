package org.wikipedia.page.references

import kotlinx.serialization.Serializable

@Serializable
data class PageReferences(val selectedIndex: Int = 0,
                          val tid: String?,
                          val referencesGroup: List<Reference>? = emptyList()) {

    @Serializable
    data class Reference(val id: String?,
                         val href: String?,
                         val text: String = "",
                         val html: String = "")
}
