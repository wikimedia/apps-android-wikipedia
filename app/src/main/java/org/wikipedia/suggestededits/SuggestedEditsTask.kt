package org.wikipedia.suggestededits

import android.graphics.drawable.Drawable

class SuggestedEditsTask {

    var title: String? = null
    var description: String? = null
    var disabled: Boolean = false
    var showImagePlaceholder: Boolean = true
    var showActionLayout: Boolean = false
    var unlockActionPositiveButtonString: String? = null
    var unlockActionNegativeButtonString: String? = null
    var unlockMessageText: String? = null
    var imageDrawable: Drawable? = null
}
