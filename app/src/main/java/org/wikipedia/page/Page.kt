package org.wikipedia.page

class Page(val title: PageTitle, var sections: List<Section> = emptyList(), var pageProperties: PageProperties) {
    val displayTitle = pageProperties.displayTitle
    val isMainPage = pageProperties.isMainPage
    val isArticle = !isMainPage && pageProperties.namespace.main()
    val isProtected = !pageProperties.canEdit
}
