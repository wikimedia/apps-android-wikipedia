package org.wikipedia.suggestededits

class ImageTag(
        val wikidataId: String,
        var label: String,
        var description: String? = null,
        var isSelected: Boolean = false
)
