package org.wikipedia.edit.insertmedia

import kotlinx.serialization.Serializable
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.PageTitle

@Serializable
data class MediaSearchResult(val pageTitle: PageTitle,
                             val imageInfo: ImageInfo?)
