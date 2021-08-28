package org.wikipedia.commons

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.databinding.FragmentFilePageBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.media.MediaHelper.getImageCaptions
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class FilePageFragment : Fragment() {
    private var _binding: FragmentFilePageBinding? = null
    private val binding get() = _binding!!
    private lateinit var pageTitle: PageTitle
    private lateinit var pageSummaryForEdit: PageSummaryForEdit
    private var suggestionReason: String? = null
    private var allowEdit = true
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = requireArguments().getParcelable(FilePageActivity.INTENT_EXTRA_PAGE_TITLE)!!
        allowEdit = requireArguments().getBoolean(FilePageActivity.INTENT_EXTRA_ALLOW_EDIT)
        suggestionReason = requireArguments().getString(FilePageActivity.INTENT_EXTRA_SUGGESTION_REASON)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == ACTIVITY_REQUEST_ADD_IMAGE_CAPTION || requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) && resultCode == RESULT_OK) {
            SuggestedEditsSnackbars.show(requireActivity(), if (requestCode == ACTIVITY_REQUEST_ADD_IMAGE_CAPTION)
                Action.ADD_CAPTION else Action.ADD_IMAGE_TAGS, requestCode == ACTIVITY_REQUEST_ADD_IMAGE_CAPTION)
            loadImageInfo()
        }
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

        disposables.add(Observable.zip(getImageCaptions(pageTitle.prefixedText),
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(pageTitle.prefixedText,
                    pageTitle.wikiSite.languageCode), { caption, response ->
                    // set image caption to pageTitle description
                    pageTitle.description = caption[pageTitle.wikiSite.languageCode]
                    response
                })
                .subscribeOn(Schedulers.io())
                .flatMap {
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
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getProtectionInfo(pageTitle.prefixedText)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    binding.filePageView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    binding.filePageView.setup(
                            this,
                            pageSummaryForEdit,
                            imageTags,
                            page,
                            binding.container.width,
                            thumbnailWidth,
                            thumbnailHeight,
                            imageFromCommons = isFromCommons,
                            showFilename = true,
                            showEditButton = allowEdit && isFromCommons && !isEditProtected,
                            suggestionReason = suggestionReason
                    )
                }
                .subscribe({
                    isEditProtected = it.query?.isEditProtected ?: false
                }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
    }

    companion object {
        const val ACTIVITY_REQUEST_ADD_IMAGE_CAPTION = 1
        const val ACTIVITY_REQUEST_ADD_IMAGE_TAGS = 2

        fun newInstance(pageTitle: PageTitle, allowEdit: Boolean, suggestionReason: String?): FilePageFragment {
            return FilePageFragment().apply {
                arguments = bundleOf(FilePageActivity.INTENT_EXTRA_PAGE_TITLE to pageTitle,
                        FilePageActivity.INTENT_EXTRA_ALLOW_EDIT to allowEdit,
                        FilePageActivity.INTENT_EXTRA_SUGGESTION_REASON to suggestionReason)
            }
        }
    }
}
