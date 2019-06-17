package org.wikipedia.views

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import kotlinx.android.synthetic.main.dialog_image_preview.*
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L


class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private lateinit var suggestedEditsSummary: SuggestedEditsSummary

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)
        suggestedEditsSummary = GsonUnmarshaller.unmarshal<SuggestedEditsSummary>(SuggestedEditsSummary::class.java, arguments!!.getString(ARG_SUMMARY))
        setConditionalLayoutDirection(rootView, suggestedEditsSummary.lang)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar!!.visibility = View.VISIBLE
        toolbarView.setOnClickListener { dismiss() }
        imagePageCommonsLinkContainer.setOnClickListener {
            dismiss()
            UriUtil.visitInExternalBrowser(context,
                    Uri.parse(String.format(getString(R.string.suggested_edits_image_file_page_commons_link), suggestedEditsSummary.title)))
        }
        setImageDetails()
    }

    override fun onDestroyView() {
        toolbarView!!.setOnClickListener(null)
        super.onDestroyView()
    }

    private fun setImageDetails() {
        loadImage(ImageUrlUtil.getUrlForPreferredSize(suggestedEditsSummary.originalUrl!!, Constants.PREFERRED_GALLERY_IMAGE_SIZE))
        titleText!!.text = StringUtil.fromHtml(StringUtil.removeNamespace(suggestedEditsSummary.title))
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_caption_title), StringUtil.fromHtml(suggestedEditsSummary.description).toString(), false)
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_artist), StringUtil.fromHtml(suggestedEditsSummary.metadata!!.artist()!!.value()).toString(), false)
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_date), suggestedEditsSummary.metadata!!.dateTime()!!.value(), false)
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_source), suggestedEditsSummary.metadata!!.imageDescription()!!.source(), true)
        addDetailPortion(getString(R.string.suggested_edits_image_preview_dialog_licensing), suggestedEditsSummary.metadata!!.licenseShortName()!!.value(), true)
        detailsHolder.requestLayout()
    }

    private fun addDetailPortion(@NonNull title: String, @Nullable detail: String?, shouldAddAccentTint: Boolean) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailView(requireContext())
            view.titleTextView.text = title
            if (shouldAddAccentTint) {
                view.detailTextView.setTextColor(ResourceUtil.getThemedColor(context!!, R.attr.colorAccent))
            }
            view.detailTextView.text = detail
            detailsHolder.addView(view)
        }
    }

    private fun loadImage(url: String?) {
        progressBar!!.visibility = View.GONE

        galleryImage.visibility = View.VISIBLE
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
