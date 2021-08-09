package org.wikipedia.views

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.commons.ImageTagsProvider
import org.wikipedia.databinding.DialogImagePreviewBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.json.MoshiUtil
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private var _binding: DialogImagePreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageSummaryForEdit: PageSummaryForEdit
    private lateinit var action: Action
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogImagePreviewBinding.inflate(inflater, container, false)
        val arguments = requireArguments()
        val adapter = MoshiUtil.getDefaultMoshi().adapter(PageSummaryForEdit::class.java)
        pageSummaryForEdit = adapter.fromJson(arguments.getString(ARG_SUMMARY, "null"))!!
        action = arguments.getSerializable(ARG_ACTION) as Action
        setConditionalLayoutDirection(binding.root, pageSummaryForEdit.lang)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.imagePreviewSheetPeekHeight))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.visibility = VISIBLE
        binding.toolbarView.setOnClickListener { dismiss() }
        binding.titleText.text = StringUtil.removeHTMLTags(StringUtil.removeNamespace(pageSummaryForEdit.displayTitle!!))
        loadImageInfo()
    }

    override fun onDestroyView() {
        binding.toolbarView.setOnClickListener(null)
        _binding = null
        disposables.clear()
        super.onDestroyView()
    }

    private fun showError(caught: Throwable?) {
        binding.dialogDetailContainer.layoutTransition = null
        binding.dialogDetailContainer.minimumHeight = 0
        binding.progressBar.visibility = GONE
        binding.filePageView.visibility = GONE
        binding.errorView.visibility = VISIBLE
        binding.errorView.setError(caught)
    }

    private fun loadImageInfo() {
        lateinit var imageTags: Map<String, List<String>>
        lateinit var page: MwQueryPage
        var isFromCommons = false
        var thumbnailWidth = 0
        var thumbnailHeight = 0

        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(pageSummaryForEdit.title, pageSummaryForEdit.lang)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    if (it.query?.firstPage?.firstImageInfo == null) {
                        // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                        ServiceFactory.get(pageSummaryForEdit.pageTitle.wikiSite).getImageInfo(pageSummaryForEdit.title, pageSummaryForEdit.lang)
                    } else {
                        // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                        isFromCommons = it.query?.firstPage?.isImageShared != true
                        Observable.just(it)
                    }
                }
                .flatMap { response ->
                    page = response.query?.firstPage!!
                    page.firstImageInfo?.let {
                        pageSummaryForEdit.timestamp = it.timestamp
                        pageSummaryForEdit.user = it.user
                        pageSummaryForEdit.metadata = it.metadata
                        thumbnailWidth = it.thumbWidth
                        thumbnailHeight = it.thumbHeight
                    }
                    ImageTagsProvider.getImageTagsObservable(page.pageId, pageSummaryForEdit.lang)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    binding.filePageView.visibility = VISIBLE
                    binding.progressBar.visibility = GONE
                    binding.filePageView.setup(
                            this,
                            pageSummaryForEdit,
                            imageTags,
                            page,
                            binding.dialogDetailContainer.width,
                            thumbnailWidth, thumbnailHeight,
                            imageFromCommons = isFromCommons,
                            showFilename = false,
                            showEditButton = false,
                            action = action
                    )
                }
                .subscribe({ imageTags = it }, { caught ->
                    L.e(caught)
                    showError(caught)
                }))
    }

    companion object {
        private const val ARG_SUMMARY = "summary"
        private const val ARG_ACTION = "action"

        fun newInstance(pageSummaryForEdit: PageSummaryForEdit, action: Action): ImagePreviewDialog {
            val adapter = MoshiUtil.getDefaultMoshi().adapter(PageSummaryForEdit::class.java)
            return ImagePreviewDialog().apply {
                arguments = bundleOf(ARG_SUMMARY to adapter.toJson(pageSummaryForEdit), ARG_ACTION to action)
            }
        }
    }
}
