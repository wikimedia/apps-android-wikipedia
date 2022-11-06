package org.wikipedia.gallery

import kotlinx.coroutines.flow.flow
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle

class GalleryRepository {

    fun fetchGalleryItems(pageTitle: PageTitle, revision: Long) = flow {
        emit(ServiceFactory.getRest(pageTitle.wikiSite).getMediaList(pageTitle.prefixedText, revision))
    }

    fun fetchEntitiesByTitle(text: String) = flow {
        emit(ServiceFactory.get(Constants.commonsWikiSite).getEntitiesByTitleSuspend(text, Constants.COMMONS_DB_NAME))
    }

    fun fetchProtectionInfo(text: String) = flow {
        emit(ServiceFactory.get(Constants.commonsWikiSite).getProtectionInfoSuspend(text))
    }
}
