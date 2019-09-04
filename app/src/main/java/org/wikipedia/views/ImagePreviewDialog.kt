package org.wikipedia.views

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_image_preview.*
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.UriUtil.handleExternalLink
import org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl
import org.wikipedia.util.log.L

class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private lateinit var suggestedEditsSummary: SuggestedEditsSummary
    private lateinit var invokeSource: InvokeSource
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)
        suggestedEditsSummary = GsonUnmarshaller.unmarshal(SuggestedEditsSummary::class.java, arguments!!.getString(ARG_SUMMARY))
        invokeSource = arguments!!.getSerializable(ARG_INVOKE_SOURCE) as InvokeSource
        setConditionalLayoutDirection(rootView, suggestedEditsSummary.lang)
        enableFullWidthDialog()
        return rootView
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(view!!.parent as View).peekHeight = DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.imagePreviewSheetPeekHeight))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar!!.visibility = VISIBLE
        toolbarView.setOnClickListener { dismiss() }
        titleText!!.text = StringUtil.removeHTMLTags(suggestedEditsSummary.displayTitle!!)
        loadImage(suggestedEditsSummary.getPreferredSizeThumbnailUrl())
        loadImageInfoIfNeeded()
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
        detailsHolder.visibility = GONE
        galleryImage.visibility = GONE
        errorView.visibility = VISIBLE
        errorView.setError(caught)
    }

    private fun loadImageInfoIfNeeded() {
        if (suggestedEditsSummary.metadata == null) {
            disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(suggestedEditsSummary.lang)).getImageExtMetadata(suggestedEditsSummary.title)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { setImageDetails() }
                    .subscribe({ response ->
                        val page = response.query()!!.pages()!![0]
                        if (page.imageInfo() != null) {
                            val imageInfo = page.imageInfo()!!
                            suggestedEditsSummary.timestamp = imageInfo.timestamp
                            suggestedEditsSummary.user = imageInfo.user
                            suggestedEditsSummary.metadata = imageInfo.metadata
                        }
                    }, { caught ->
                        L.e(caught)
                        showError(caught)
                    }))
        } else {
            setImageDetails()
        }
    }

    private fun setImageDetails() {
        if ((invokeSource == InvokeSource.SUGGESTED_EDITS_ADD_CAPTION || invokeSource == InvokeSource.FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION)
                && suggestedEditsSummary.pageTitle.description.isNullOrEmpty()) {
            // Show the image description when a structured caption does not exist.
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_description_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(suggestedEditsSummary.lang)),
                    suggestedEditsSummary.description)
        } else {
            addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_caption_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(suggestedEditsSummary.lang)),
                    if (suggestedEditsSummary.pageTitle.description.isNullOrEmpty()) suggestedEditsSummary.description
                    else suggestedEditsSummary.pageTitle.description)
        }
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_artist), suggestedEditsSummary.metadata!!.artist())
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_date), suggestedEditsSummary.metadata!!.dateTime())
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_source), suggestedEditsSummary.metadata!!.credit())
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_licensing), suggestedEditsSummary.metadata!!.licenseShortName(), suggestedEditsSummary.metadata!!.licenseUrl())
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_more_info), getString(R.string.suggested_edits_image_preview_dialog_file_page_link_text), getString(R.string.suggested_edits_image_file_page_commons_link, suggestedEditsSummary.title))
        detailsHolder.requestLayout()
    }

    private fun addDetailPortion(titleString: String, detail: String?) {
        addDetailPortion(titleString, detail, null)
    }

    private fun addDetailPortion(titleString: String, detail: String?, externalLink: String?) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailView(requireContext())
            view.titleTextView.text = titleString
            view.detailTextView.text = StringUtil.strip(StringUtil.fromHtml(detail))
            if (!externalLink.isNullOrEmpty()) {
                view.detailTextView.setTextColor(ResourceUtil.getThemedColor(context!!, R.attr.colorAccent))
                view.detailTextView.setTextIsSelectable(false)
                view.externalLinkView.visibility = VISIBLE
                view.detailsContainer.setOnClickListener {
                    dismiss()
                    UriUtil.visitInExternalBrowser(context, Uri.parse(externalLink))
                }
            } else {
                view.detailTextView.movementMethod = movementMethod
            }
            detailsHolder.addView(view)
        }
    }

    private val movementMethod = LinkMovementMethodExt { url: String ->
        handleExternalLink(context, Uri.parse(resolveProtocolRelativeUrl(url)))
    }

    private fun loadImage(url: String?) {
        progressBar!!.visibility = GONE
        galleryImage.visibility = VISIBLE
        L.v("Loading image from url: $url")
        ViewUtil.loadImageUrlInto(galleryImage, url)
    }

    companion object {
        private const val ARG_SUMMARY = "summary"
        private const val ARG_INVOKE_SOURCE = "invokeSource"

        fun newInstance(suggestedEditsSummary: SuggestedEditsSummary, invokeSource: InvokeSource): ImagePreviewDialog {
            val dialog = ImagePreviewDialog()
            val args = Bundle()
            args.putString(ARG_SUMMARY, GsonMarshaller.marshal(suggestedEditsSummary))
            args.putSerializable(ARG_INVOKE_SOURCE, invokeSource)
            dialog.arguments = args
            return dialog
        }
    }
}
