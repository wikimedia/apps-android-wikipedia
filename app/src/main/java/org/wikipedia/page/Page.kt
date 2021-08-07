package org.wikipedia.page

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Page(var title: PageTitle, var sections: List<Section> = emptyList(), var pageProperties: PageProperties) {
    val displayTitle = pageProperties.displayTitle.orEmpty()
    val isMainPage = pageProperties.isMainPage
    val isArticle = !isMainPage && title.namespace() === Namespace.MAIN
    val isProtected = !pageProperties.canEdit()
}
