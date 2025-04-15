package org.wikipedia.yearinreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class YearInReviewDialog : ExtendedBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstance: Bundle?
    ): View? {
        (dialog as? BottomSheetDialog)?.behavior.apply {
            this?.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    YearInReviewBottomSheet()
                }
            }
        }
    }

    companion object {
        fun newInstance(): YearInReviewDialog {
            return YearInReviewDialog()
        }
    }
}
