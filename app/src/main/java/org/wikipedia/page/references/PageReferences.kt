package org.wikipedia.page.references

import kotlinx.serialization.Serializable

@Serializable
class PageReferences(val selectedIndex: Int = 0,
                     val tid: String? = null,
                     val referencesGroup: List<Reference> = emptyList()) {

    @Serializable
    class Reference(val id: String? = null,
                    val href: String? = null,
                    val text: String = "",
                    val html: String = "")
}
