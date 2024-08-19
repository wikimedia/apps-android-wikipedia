package org.wikipedia.commons

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.databinding.DialogImagePreviewBinding
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil

class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private var _binding: DialogImagePreviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ImagePreviewViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogImagePreviewBinding.inflate(inflater, container, false)
        setConditionalLayoutDirection(binding.root, viewModel.pageSummaryForEdit.lang)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.imagePreviewSheetPeekHeight))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarView.setOnClickListener { dismiss() }
        binding.titleText.text = StringUtil.removeHTMLTags(StringUtil.removeNamespace(viewModel.pageSummaryForEdit.displayTitle))

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> onSuccess(it.data)
                            is Resource.Error -> onError(it.throwable)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.toolbarView.setOnClickListener(null)
        _binding = null
        super.onDestroyView()
    }

    private fun onLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun onError(caught: Throwable?) {
        binding.dialogDetailContainer.layoutTransition = null
        binding.dialogDetailContainer.minimumHeight = 0
        binding.progressBar.visibility = View.GONE
        binding.filePageView.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.errorView.setError(caught, viewModel.pageSummaryForEdit.pageTitle)
    }

    private fun onSuccess(filePage: FilePage) {
            binding.filePageView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.filePageView.setup(
                viewModel.pageSummaryForEdit,
                filePage.imageTags,
                filePage.page,
                binding.dialogDetailContainer.width,
                filePage.thumbnailWidth,
                filePage.thumbnailHeight,
                imageFromCommons = filePage.imageFromCommons,
                showFilename = filePage.showFilename,
                showEditButton = filePage.showEditButton,
                action = viewModel.action
            )
    }

    companion object {
        const val ARG_SUMMARY = "summary"
        const val ARG_ACTION = "action"

        fun newInstance(pageSummaryForEdit: PageSummaryForEdit, action: Action? = null): ImagePreviewDialog {
            val dialog = ImagePreviewDialog().apply {
                arguments = bundleOf(ARG_SUMMARY to pageSummaryForEdit, ARG_ACTION to action)
            }
            return dialog
        }
    }
}
