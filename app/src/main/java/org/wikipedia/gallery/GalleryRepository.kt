package org.wikipedia.gallery

import kotlinx.coroutines.flow.flow
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle

class GalleryRepository {

    suspend fun fetchGalleryItems(pageTitle: PageTitle, revision: Long) = flow {
        emit(ServiceFactory.getRest(pageTitle.wikiSite).getMediaList(pageTitle.prefixedText, revision))
    }
}
