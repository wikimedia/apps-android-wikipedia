package org.wikipedia.games.onthisday

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.databinding.DialogOnThisDayGameArticleBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class OnThisDayGameArticleDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private var _binding: DialogOnThisDayGameArticleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogOnThisDayGameArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            peekHeight = 0
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: pending implementation
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


    companion object {
        const val ARG_SUMMARY = "summary"

        fun newInstance(pageSummary: PageSummary): OnThisDayGameArticleDialog {
            val dialog = OnThisDayGameArticleDialog().apply {
                arguments = bundleOf(ARG_SUMMARY to pageSummary)
            }
            return dialog
        }
    }
}
