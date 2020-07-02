package org.wikipedia.edits

import androidx.annotation.DrawableRes

class EditsTask {
    var title: String? = null
    var description: String? = null
    var disabled: Boolean = false
    var new: Boolean = false
    var translatable: Boolean = true
    @DrawableRes var imageDrawable: Int = 0
}
