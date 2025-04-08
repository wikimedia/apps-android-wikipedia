package org.wikipedia.yearinreview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.databinding.DialogYearInReviewBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class YearInReviewDialog : ExtendedBottomSheetDialogFragment() {

    private var _binding: DialogYearInReviewBinding? = null
    private val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstance: Bundle?
    ): View {
        _binding = DialogYearInReviewBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.yearInReviewLayout.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BaseTheme {
                    YearInReviewBottomSheetScaffold()
                }
            }
        }
        (dialog as? BottomSheetDialog)?.behavior.apply {
            this?.isFitToContents = true
            this?.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return view
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): YearInReviewDialog {
            return YearInReviewDialog()
        }
    }
}
