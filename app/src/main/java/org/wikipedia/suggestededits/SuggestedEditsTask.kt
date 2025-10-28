package org.wikipedia.suggestededits

import androidx.annotation.DrawableRes

class SuggestedEditsTask {
    var title: String? = null
    var description: String? = null
    var primaryAction: String? = null
    @DrawableRes
    var primaryActionIcon: Int = 0
    var secondaryAction: String? = null
    var disabled: Boolean = false

    var new: Boolean = false

    @DrawableRes
    var imageDrawable: Int = 0
}
