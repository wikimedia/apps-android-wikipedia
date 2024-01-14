package org.wikipedia.page

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

object ExclusiveBottomSheetPresenter {
    const val BOTTOM_SHEET_FRAGMENT_TAG = "bottom_sheet_fragment"
    fun show(manager: FragmentManager, dialog: DialogFragment) {
        if (manager.isStateSaved || manager.isDestroyed) {
            return
        }
        dismiss(manager)
        dialog.show(manager, BOTTOM_SHEET_FRAGMENT_TAG)
    }

    fun getCurrentBottomSheet(manager: FragmentManager): DialogFragment? {
        if (manager.isStateSaved || manager.isDestroyed) {
            return null
        }
        return manager.findFragmentByTag(BOTTOM_SHEET_FRAGMENT_TAG) as DialogFragment?
    }

    fun dismiss(manager: FragmentManager) {
        getCurrentBottomSheet(manager)?.dismiss()
    }
}
