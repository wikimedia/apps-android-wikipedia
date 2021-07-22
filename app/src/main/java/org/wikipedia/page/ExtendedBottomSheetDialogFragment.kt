package org.wikipedia.page

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class ExtendedBottomSheetDialogFragment : BottomSheetDialogFragment() {
    protected fun disableBackgroundDim() {
        requireDialog().window?.setDimAmount(0f)
    }
}
