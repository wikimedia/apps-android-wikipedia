package org.wikipedia.editactionfeed

import android.graphics.drawable.Drawable

class EditTask {

    var title: String? = null
    var description: String? = null
    var disabled: Boolean = false
    var imagePlaceHolderShown: Boolean = false
    var noActionLayout: Boolean = false
    var enabledPositiveActionString: String? = null
    var enabledNegativeActionString: String? = null
    var disabledDescriptionText: String? = null
    var imageDrawable: Drawable? = null
}
