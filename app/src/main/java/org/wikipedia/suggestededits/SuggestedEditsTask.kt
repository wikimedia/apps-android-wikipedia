package org.wikipedia.suggestededits

import android.graphics.drawable.Drawable

class SuggestedEditsTask {

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
