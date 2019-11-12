package org.wikipedia.suggestededits

import androidx.annotation.DrawableRes

class SuggestedEditsTask {
    var title: String? = null
    var description: String? = null
    var disabled: Boolean = false
    var showImagePlaceholder: Boolean = true
    var showActionLayout: Boolean = false
    var unlockActionPositiveButtonString: String? = null
    var unlockActionNegativeButtonString: String? = null
    var unlockMessageText: String? = null
    @DrawableRes var imageDrawable: Int = 0
}
