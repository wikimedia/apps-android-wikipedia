package org.wikipedia.views

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.Nullable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_image_preview.*
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.R
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private lateinit var suggestedEditsSummary: SuggestedEditsSummary
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)
        suggestedEditsSummary = GsonUnmarshaller.unmarshal<SuggestedEditsSummary>(SuggestedEditsSummary::class.java, arguments!!.getString(ARG_SUMMARY))
        setConditionalLayoutDirection(rootView, suggestedEditsSummary.lang)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar!!.visibility = VISIBLE
        toolbarView.setOnClickListener { dismiss() }
        imagePageCommonsLinkContainer.setOnClickListener {
            dismiss()
            UriUtil.visitInExternalBrowser(context,
                    Uri.parse(String.format(getString(R.string.suggested_edits_image_file_page_commons_link), suggestedEditsSummary.title)))
        }

        titleText!!.text = StringUtil.fromHtml(StringUtil.removeNamespace(suggestedEditsSummary.title))
        loadImage(suggestedEditsSummary.thumbnailUrl)
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
        moreInfoContainer.visibility = GONE
        errorView.visibility = VISIBLE
        errorView.setError(caught)
    }

    private fun loadImageInfoIfNeeded() {
        if (suggestedEditsSummary.metadata == null) {
            disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(suggestedEditsSummary.lang)).getImageExtMetadata(suggestedEditsSummary.title)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally{ setImageDetails() }
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
        addDetailPortion(R.string.suggested_edits_image_preview_dialog_caption_title, suggestedEditsSummary.description, false)
        addDetailPortion(R.string.suggested_edits_image_preview_dialog_artist, suggestedEditsSummary.metadata!!.artist(), false)
        addDetailPortion(R.string.suggested_edits_image_preview_dialog_date, suggestedEditsSummary.metadata!!.dateTime(), false)
        addDetailPortion(R.string.suggested_edits_image_preview_dialog_source, suggestedEditsSummary.metadata!!.imageDescriptionSource(), true)
        addDetailPortion(R.string.suggested_edits_image_preview_dialog_licensing, suggestedEditsSummary.metadata!!.licenseShortName(), true)
        detailsHolder.requestLayout()
    }

    private fun addDetailPortion(titleRes: Int, @Nullable detail: String?, shouldAddAccentTint: Boolean) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailView(requireContext())
            view.titleTextView.text = getString(titleRes)
            if (shouldAddAccentTint) {
                view.detailTextView.setTextColor(ResourceUtil.getThemedColor(context!!, R.attr.colorAccent))
            }
            view.detailTextView.text = StringUtil.fromHtml(detail)
            detailsHolder.addView(view)
        }
    }

    private fun loadImage(url: String?) {
        progressBar!!.visibility = GONE
        galleryImage.visibility = VISIBLE
        L.v("Loading image from url: $url")
        ViewUtil.loadImageUrlInto(galleryImage, url)
    }

    companion object {
        private const val ARG_SUMMARY = "summary"

        fun newInstance(suggestedEditsSummary: SuggestedEditsSummary): ImagePreviewDialog {
            val dialog = ImagePreviewDialog()
            val args = Bundle()
            args.putString(ARG_SUMMARY, GsonMarshaller.marshal(suggestedEditsSummary))
            dialog.arguments = args
            return dialog
        }
    }
}
