package org.wikipedia.commons

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.databinding.FragmentFilePageBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.gallery.ImagePipelineBitmapGetter
import org.wikipedia.gallery.MediaDownloadReceiver
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ShareUtil.shareImage
import java.io.File

class FilePageFragment : Fragment(), FilePageView.Callback, MenuProvider {
    private var _binding: FragmentFilePageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FilePageViewModel by viewModels()

    private val downloadReceiver = MediaDownloadReceiver()
    private val downloadReceiverCallback = MediaDownloadReceiverCallback()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            downloadImage()
        } else {
            FeedbackUtil.showMessage(requireActivity(), R.string.gallery_save_image_write_permission_rationale)
        }
    }

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
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

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

    private fun onSuccess(filePage: FilePage) {
        viewModel.pageSummaryForEdit?.let {
            binding.filePageView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.filePageView.setup(
                it,
                filePage.imageTags,
                filePage.page,
                DimenUtil.displayWidthPx,
                filePage.thumbnailWidth,
                filePage.thumbnailHeight,
                imageFromCommons = filePage.imageFromCommons,
                showFilename = filePage.showFilename,
                showEditButton = filePage.showEditButton,
                callback = this
            )
        }
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        downloadReceiver.unregister(requireContext())
    }

    override fun onResume() {
        super.onResume()
        downloadReceiver.register(requireContext(), downloadReceiverCallback)
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_file_page, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        if (!isAdded) {
            return
        }
        val enableShareAndSave = viewModel.mediaInfo?.thumbUrl?.isNotEmpty() == true
        menu.findItem(R.id.menu_file_save).isEnabled = enableShareAndSave
        menu.findItem(R.id.menu_file_share).isEnabled = enableShareAndSave
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_file_save -> {
                handleImageSaveRequest()
                true
            }
            R.id.menu_file_share -> {
                shareImage()
                true
            }
            else -> false
        }
    }

    private fun handleImageSaveRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            downloadImage()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun shareImage() {
        viewModel.pageSummaryForEdit?.let { summary ->
            val thumbUrl = summary.getPreferredSizeThumbnailUrl()
            ImagePipelineBitmapGetter(requireContext(), thumbUrl) { bitmap ->
                if (!isAdded) {
                    return@ImagePipelineBitmapGetter
                }
                shareImage(lifecycleScope, requireContext(), bitmap, File(thumbUrl).name,
                    summary.displayTitle, summary.pageTitle.uri)
            }
        }
    }

    private fun downloadImage() {
        viewModel.pageSummaryForEdit?.let { summary ->
            viewModel.mediaInfo?.let { info ->
                downloadReceiver.download(requireContext(), summary.pageTitle, info)
                FeedbackUtil.showMessage(this, R.string.gallery_save_progress)
            }
        } ?: run {
            FeedbackUtil.showMessage(this, R.string.err_cannot_save_file)
        }
    }

    private inner class MediaDownloadReceiverCallback : MediaDownloadReceiver.Callback {
        override fun onSuccess() {
            FeedbackUtil.showMessage(this@FilePageFragment, R.string.gallery_save_success)
        }
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
