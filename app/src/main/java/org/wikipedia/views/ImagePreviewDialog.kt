package org.wikipedia.views

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
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
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.edits.EditsSummary
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private lateinit var editsSummary: EditsSummary
    private lateinit var action: Action
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)
        editsSummary = GsonUnmarshaller.unmarshal(EditsSummary::class.java, requireArguments().getString(ARG_SUMMARY))
        action = requireArguments().getSerializable(ARG_ACTION) as Action
        setConditionalLayoutDirection(rootView, editsSummary.lang)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.imagePreviewSheetPeekHeight))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar!!.visibility = VISIBLE
        toolbarView.setOnClickListener { dismiss() }
        titleText!!.text = StringUtil.removeHTMLTags(StringUtil.removeNamespace(editsSummary.displayTitle!!))
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
        progressBar.visibility = GONE
        filePageView.visibility = GONE
        errorView.visibility = VISIBLE
        errorView.setError(caught)
    }

    private fun loadImageInfo() {
        lateinit var imageTags: Map<String, List<String>>
        var isFromCommons = false
        var thumbnailWidth = 0
        var thumbnailHeight = 0

        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(editsSummary.title, editsSummary.lang)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    if (it.query()!!.pages()!![0].imageInfo() == null) {
                        // If file page originally comes from *.wikipedia.org (i.e. movie posters), it will not have imageInfo and pageId.
                        ServiceFactory.get(editsSummary.pageTitle.wikiSite).getImageInfo(editsSummary.title, editsSummary.lang)
                    } else {
                        // Fetch API from commons.wikimedia.org and check whether if it is not a "shared" image.
                        isFromCommons = !it.query()!!.pages()!![0].isImageShared
                        Observable.just(it)
                    }
                }
                .flatMap { response ->
                    val page = response.query()!!.pages()!![0]
                    if (page.imageInfo() != null) {
                        val imageInfo = page.imageInfo()!!
                        editsSummary.timestamp = imageInfo.timestamp
                        editsSummary.user = imageInfo.user
                        editsSummary.metadata = imageInfo.metadata
                        thumbnailWidth = imageInfo.thumbWidth
                        thumbnailHeight = imageInfo.thumbHeight
                    }
                    ImageTagsProvider.getImageTagsObservable(page.pageId(), editsSummary.lang)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    filePageView.visibility = VISIBLE
                    progressBar.visibility = GONE
                    filePageView.setup(
                            editsSummary,
                            imageTags,
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

        fun newInstance(editsSummary: EditsSummary, action: Action): ImagePreviewDialog {
            val dialog = ImagePreviewDialog()
            val args = Bundle()
            args.putString(ARG_SUMMARY, GsonMarshaller.marshal(editsSummary))
            args.putSerializable(ARG_ACTION, action)
            dialog.arguments = args
            return dialog
        }
    }
}
