package org.wikipedia.commons

import org.wikipedia.dataclient.mwapi.MwQueryPage

class FilePage(
    val thumbnailWidth: Int = 0,
    val thumbnailHeight: Int = 0,
    val imageFromCommons: Boolean = false,
    val showEditButton: Boolean = false,
    val showFilename: Boolean = false,
    val page: MwQueryPage = MwQueryPage(),
    val imageTags: Map<String, List<String>> = emptyMap()
)
