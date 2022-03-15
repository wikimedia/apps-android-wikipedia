package org.wikipedia.page

class Page(var title: PageTitle, var sections: List<Section> = emptyList(), var pageProperties: PageProperties) {
    val displayTitle = pageProperties.displayTitle
    val isMainPage = pageProperties.isMainPage
    val isArticle = !isMainPage && title.namespace() === Namespace.MAIN
    val isProtected = !pageProperties.canEdit
}
