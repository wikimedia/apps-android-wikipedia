package org.wikipedia.commons

import org.wikipedia.dataclient.mwapi.MwQueryPage

class FilePage {
    var thumbnailWidth = 0
    var thumbnailHeight = 0
    var imageFromCommons = false
    var showEditButton = false
    var showFilename = false
    var page = MwQueryPage()
    var imageTags = emptyMap<String, List<String>>()
}
