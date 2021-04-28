package org.wikipedia.page

class Page(var title: PageTitle,
           var sections: List<Section>,
           var pageProperties: PageProperties) {

    constructor(title: PageTitle, pageProperties: PageProperties) : this(title, emptyList(), pageProperties)

    val displayTitle = pageProperties.displayTitle.orEmpty()
    val isMainPage = pageProperties.isMainPage
    val isArticle = !isMainPage && title.namespace() === Namespace.MAIN
    val isProtected = !pageProperties.canEdit()
}
