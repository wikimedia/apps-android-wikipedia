package org.wikipedia.edit.insertmedia

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.PageTitle

@Serializable
data class MediaSearchResult(val pageTitle: PageTitle,
                             val imageInfo: ImageInfo?)
