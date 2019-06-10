package org.wikipedia.views

import android.content.DialogInterface
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import kotlinx.android.synthetic.main.dialog_image_preview.*
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)
        suggestedEditsSummary = GsonUnmarshaller.unmarshal<SuggestedEditsSummary>(SuggestedEditsSummary::class.java, arguments!!.getString(SUMMARY))
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
        loadImage(suggestedEditsSummary.originalUrl)
        titleText!!.text = StringUtil.fromHtml(StringUtil.removeNamespace(suggestedEditsSummary.title))
        addDetailPortion(getString(R.string.suggested_edits_image_caption_title), StringUtil.fromHtml(suggestedEditsSummary.description).toString(), false)
        addDetailPortion(getString(R.string.suggested_edits_image_artist), StringUtil.fromHtml(suggestedEditsSummary.metadata!!.artist()!!.value()).toString(), false)
        addDetailPortion(getString(R.string.suggested_edits_image_date), suggestedEditsSummary.metadata!!.dateTime()!!.value(), false)
        addDetailPortion(getString(R.string.suggested_edits_image_source), suggestedEditsSummary.metadata!!.imageDescription()!!.source(), true)
        addDetailPortion(getString(R.string.suggested_edits_image_licensing), suggestedEditsSummary.metadata!!.licenseShortName()!!.value(), true)
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

        galleryImage.controller = Fresco.newDraweeControllerBuilder()
                .setUri(url)
                .setAutoPlayAnimations(true)
                .setControllerListener(object : BaseControllerListener<com.facebook.imagepipeline.image.ImageInfo>() {
                    override fun onFinalImageSet(id: String?, imageInfo: com.facebook.imagepipeline.image.ImageInfo?, animatable: Animatable?) {
                        galleryImage.setDrawBackground(true)
                    }

                    override fun onFailure(id: String?, throwable: Throwable?) {
                        L.d(throwable)
                    }
                })
                .build()
    }

    companion object {
        private const val SUMMARY = "summary"

        fun newInstance(suggestedEditsSummary: SuggestedEditsSummary): ImagePreviewDialog {
            val dialog = ImagePreviewDialog()
            val args = Bundle()
            args.putString(SUMMARY, GsonMarshaller.marshal(suggestedEditsSummary))
            dialog.arguments = args
            return dialog
        }

    }
}
