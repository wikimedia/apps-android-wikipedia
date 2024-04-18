package org.wikipedia.commons

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.databinding.FragmentFilePageBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource

class FilePageFragment : Fragment(), FilePageView.Callback {
    private var _binding: FragmentFilePageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilePageViewModel by viewModels { FilePageViewModel.Factory(requireArguments()) }

    private val addImageCaptionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            SuggestedEditsSnackbars.show(requireActivity(), Action.ADD_CAPTION, true)
            viewModel.loadImageInfo()
        }
    }

    private val addImageTagsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            SuggestedEditsSnackbars.show(requireActivity(), Action.ADD_IMAGE_TAGS, true)
            viewModel.loadImageInfo()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentFilePageBinding.inflate(inflater, container, false)
        L10nUtil.setConditionalLayoutDirection(binding.root, viewModel.pageTitle.wikiSite.languageCode)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.loadImageInfo()
        }
        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
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
        ImageRecommendationsEvent.logImpression("imagedetails_dialog", ImageRecommendationsEvent.getActionDataString(filename = viewModel.pageTitle.prefixedText))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun onError(caught: Throwable?) {
        binding.progressBar.visibility = View.GONE
        binding.filePageView.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.errorView.setError(caught)
    }

    private fun onLoading() {
        binding.errorView.visibility = View.GONE
        binding.filePageView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun onSuccess(filePage: FilePageViewModel.FilePage) {
        viewModel.pageSummaryForEdit?.let {
            binding.filePageView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.filePageView.setup(
                it,
                filePage.imageTags,
                filePage.page,
                binding.container.width,
                filePage.thumbnailWidth,
                filePage.thumbnailHeight,
                imageFromCommons = filePage.imageFromCommons,
                showFilename = filePage.showFilename,
                showEditButton = filePage.showEditButton,
                callback = this
            )
        }
    }

    override fun onImageCaptionClick(summaryForEdit: PageSummaryForEdit) {
        viewModel.pageSummaryForEdit?.let {
            addImageCaptionLauncher.launch(
                DescriptionEditActivity.newIntent(requireContext(),
                    it.pageTitle, null, summaryForEdit, null,
                    Action.ADD_CAPTION, Constants.InvokeSource.FILE_PAGE_ACTIVITY)
            )
        }
    }

    override fun onImageTagsClick(page: MwQueryPage) {
        addImageTagsLauncher.launch(
            SuggestedEditsImageTagEditActivity.newIntent(requireContext(), page, Constants.InvokeSource.FILE_PAGE_ACTIVITY)
        )
    }

    companion object {
        fun newInstance(pageTitle: PageTitle, allowEdit: Boolean): FilePageFragment {
            return FilePageFragment().apply {
                arguments = bundleOf(Constants.ARG_TITLE to pageTitle,
                        FilePageActivity.INTENT_EXTRA_ALLOW_EDIT to allowEdit)
            }
        }
    }
}
