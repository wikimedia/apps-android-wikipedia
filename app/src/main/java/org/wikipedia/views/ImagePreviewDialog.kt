package org.wikipedia.views

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_image_preview.*
import org.wikipedia.R
import org.wikipedia.commons.ImageTagsProvider
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private lateinit var pageSummaryForEdit: PageSummaryForEdit
    private lateinit var action: Action
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)
        pageSummaryForEdit = GsonUnmarshaller.unmarshal(PageSummaryForEdit::class.java, requireArguments().getString(ARG_SUMMARY))
        action = requireArguments().getSerializable(ARG_ACTION) as Action
        setConditionalLayoutDirection(rootView, pageSummaryForEdit.lang)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.imagePreviewSheetPeekHeight))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar!!.isVisible = true
        toolbarView.setOnClickListener { dismiss() }
        titleText!!.text = StringUtil.removeHTMLTags(StringUtil.removeNamespace(pageSummaryForEdit.displayTitle!!))
        loadImageInfo()
    }

    override fun onDestroyView() {
        toolbarView!!.setOnClickListener(null)
        disposables.clear()
        super.onDestroyView()
    }

    private fun showError(caught: Throwable?) {
        dialogDetailContainer.layoutTransition = null
        dialogDetailContainer.minimumHeight = 0
        progressBar.isVisible = false
        filePageView.isVisible = false
        errorView.isVisible = true
        errorView.setError(caught)
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
                    if (it.query()!!.pages()!![0].imageInfo() == null) {
                        // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                        ServiceFactory.get(pageSummaryForEdit.pageTitle.wikiSite).getImageInfo(pageSummaryForEdit.title, pageSummaryForEdit.lang)
                    } else {
                        // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                        isFromCommons = !it.query()!!.pages()!![0].isImageShared
                        Observable.just(it)
                    }
                }
                .flatMap { response ->
                    page = response.query()!!.pages()!![0]
                    if (page.imageInfo() != null) {
                        val imageInfo = page.imageInfo()!!
                        pageSummaryForEdit.timestamp = imageInfo.timestamp
                        pageSummaryForEdit.user = imageInfo.user
                        pageSummaryForEdit.metadata = imageInfo.metadata
                        thumbnailWidth = imageInfo.thumbWidth
                        thumbnailHeight = imageInfo.thumbHeight
                    }
                    ImageTagsProvider.getImageTagsObservable(page.pageId(), pageSummaryForEdit.lang)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    filePageView.isVisible = true
                    progressBar.isVisible = false
                    filePageView.setup(
                            this,
                            pageSummaryForEdit,
                            imageTags,
                            page,
                            dialogDetailContainer.width,
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
            val dialog = ImagePreviewDialog()
            dialog.arguments = bundleOf(ARG_SUMMARY to GsonMarshaller.marshal(pageSummaryForEdit),
                    ARG_ACTION to action)
            return dialog
        }
    }
}
