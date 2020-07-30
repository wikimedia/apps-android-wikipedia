package org.wikipedia.suggestededits

import androidx.annotation.DrawableRes

class SuggestedEditsTask {
    var title: String? = null
    var description: String? = null
    var disabled: Boolean = false
    var new: Boolean = false
    var translatable: Boolean = true
    @DrawableRes
    var imageDrawable: Int = 0
}
