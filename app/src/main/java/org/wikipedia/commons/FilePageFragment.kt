package org.wikipedia.commons

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentFilePageBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.language.LanguageUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class FilePageFragment : Fragment(), FilePageView.Callback {
    private var _binding: FragmentFilePageBinding? = null
    private val binding get() = _binding!!
    private lateinit var pageTitle: PageTitle
    private lateinit var pageSummaryForEdit: PageSummaryForEdit
    private var allowEdit = true
    private val disposables = CompositeDisposable()

    private val addImageCaptionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            SuggestedEditsSnackbars.show(requireActivity(), Action.ADD_CAPTION, true)
            loadImageInfo()
        }
    }

    private val addImageTagsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            SuggestedEditsSnackbars.show(requireActivity(), Action.ADD_IMAGE_TAGS, true)
            loadImageInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = requireArguments().getParcelable(Constants.ARG_TITLE)!!
        allowEdit = requireArguments().getBoolean(FilePageActivity.INTENT_EXTRA_ALLOW_EDIT)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentFilePageBinding.inflate(inflater, container, false)
        L10nUtil.setConditionalLayoutDirection(container!!, pageTitle.wikiSite.languageCode)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            loadImageInfo()
        }
        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        loadImageInfo()
    }

    override fun onDestroyView() {
        disposables.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun showError(caught: Throwable?) {
        binding.progressBar.visibility = View.GONE
        binding.filePageView.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
        binding.errorView.setError(caught)
    }

    private fun loadImageInfo() {
        lateinit var imageTags: Map<String, List<String>>
        lateinit var page: MwQueryPage
        var isFromCommons = false
        var isEditProtected = false
        var thumbnailWidth = 0
        var thumbnailHeight = 0

        binding.errorView.visibility = View.GONE
        binding.filePageView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        disposables.add(ServiceFactory.get(Constants.commonsWikiSite).getImageInfoWithEntityTerms(pageTitle.prefixedText, pageTitle.wikiSite.languageCode, LanguageUtil.convertToUselangIfNeeded(pageTitle.wikiSite.languageCode))
                .subscribeOn(Schedulers.io())
                .flatMap {
                    // set image caption to pageTitle description
                    pageTitle.description = it.query?.firstPage()?.entityTerms?.label?.firstOrNull()
                    if (it.query?.firstPage()?.imageInfo() == null) {
                        // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                        ServiceFactory.get(pageTitle.wikiSite).getImageInfo(pageTitle.prefixedText, pageTitle.wikiSite.languageCode)
                    } else {
                        // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                        isFromCommons = !(it.query?.firstPage()?.isImageShared ?: false)
                        Observable.just(it)
                    }
                }
                .subscribeOn(Schedulers.io())
                .flatMap {
                    page = it.query?.firstPage()!!
                    val imageInfo = page.imageInfo()!!
                    pageSummaryForEdit = PageSummaryForEdit(
                            pageTitle.prefixedText,
                            pageTitle.wikiSite.languageCode,
                            pageTitle,
                            pageTitle.displayText,
                            StringUtil.fromHtml(imageInfo.metadata!!.imageDescription()).toString().ifBlank { null },
                            imageInfo.thumbUrl,
                            null,
                            null,
                            imageInfo.timestamp,
                            imageInfo.user,
                            imageInfo.metadata
                    )
                    thumbnailHeight = imageInfo.thumbHeight
                    thumbnailWidth = imageInfo.thumbWidth
                    ImageTagsProvider.getImageTagsObservable(page.pageId, pageSummaryForEdit.lang)
                }
                .flatMap {
                    imageTags = it
                    ServiceFactory.get(Constants.commonsWikiSite).getProtectionInfo(pageTitle.prefixedText)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    binding.filePageView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    binding.filePageView.setup(
                            pageSummaryForEdit,
                            imageTags,
                            page,
                            binding.container.width,
                            thumbnailWidth,
                            thumbnailHeight,
                            imageFromCommons = isFromCommons,
                            showFilename = true,
                            showEditButton = allowEdit && isFromCommons && !isEditProtected,
                            callback = this
                    )
                }
                .subscribe({
                    isEditProtected = it.query?.isEditProtected ?: false
                }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
    }

    override fun onImageCaptionClick(summaryForEdit: PageSummaryForEdit) {
        addImageCaptionLauncher.launch(
            DescriptionEditActivity.newIntent(requireContext(),
                pageSummaryForEdit.pageTitle, null, summaryForEdit, null,
            Action.ADD_CAPTION, Constants.InvokeSource.FILE_PAGE_ACTIVITY)
        )
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
