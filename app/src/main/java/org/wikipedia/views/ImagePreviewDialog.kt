package org.wikipedia.views

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.PopupMenu
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import kotlinx.android.synthetic.main.dialog_image_preview.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L


class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {


    private var fileName: String? = null
    private lateinit var imageInfo: ImageInfo

    private val menuListener = PopupMenu.OnMenuItemClickListener { item ->
        false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val app = WikipediaApp.getInstance()

        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)

        setConditionalLayoutDirection(rootView, WikipediaApp.getInstance().language().appLanguageCode)
        imageInfo = arguments!!.getSerializable(IMAGE_INFO) as ImageInfo
        fileName = arguments!!.getString(FILE_NAME)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        overflowButton!!.setOnClickListener { v: View ->
            val popupMenu = PopupMenu(requireActivity(), overflowButton!!)
            popupMenu.inflate(R.menu.menu_link_preview)
            popupMenu.setOnMenuItemClickListener(menuListener)
            popupMenu.show()
        }

        // show the progress bar while we load content...
        progressBar!!.visibility = View.VISIBLE
        toolbarView.setOnClickListener { dismiss() }
        imagePageCommonsLinkContainer.setOnClickListener {
            dismiss()
            UriUtil.visitInExternalBrowser(context,
                    Uri.parse(String.format(getString(R.string.suggested_edits_image_file_page_commons_link), fileName)))
        }
        setImageDetails()
    }

    override fun onDestroyView() {
        toolbarView!!.setOnClickListener(null)
        overflowButton!!.setOnClickListener(null)
        super.onDestroyView()
    }

    private fun setImageDetails() {
        loadImage(imageInfo.originalUrl)
        titleText!!.text = StringUtil.fromHtml(fileName!!.removePrefix("File:"))
        addDetailPortion(getString(R.string.suggested_edits_image_caption_title), StringUtil.fromHtml(imageInfo.metadata!!.imageDescription()!!.value()).toString())
        addDetailPortion(getString(R.string.suggested_edits_image_artist), StringUtil.fromHtml(imageInfo.metadata!!.artist()!!.value()).toString())
        addDetailPortion(getString(R.string.suggested_edits_image_date), imageInfo.metadata!!.dateTime()!!.value())
        addDetailPortion(getString(R.string.suggested_edits_image_source), imageInfo.metadata!!.imageDescription()!!.source())
        addDetailPortion(getString(R.string.suggested_edits_image_licensing), imageInfo.metadata!!.licenseShortName()!!.value())
        detailsHolder.requestLayout()
    }

    private fun addDetailPortion(@NonNull title: String, @Nullable detail: String?) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailView(requireContext())
            view.setTitle(title)
            view.setDetail(detail)
            detailsHolder.addView(view)
        }
    }

    override fun onDismiss(dialogInterface: DialogInterface?) {
        super.onDismiss(dialogInterface)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED) {
            startActivity(data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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
        const val IMAGE_INFO = "imageInfo"
        const val FILE_NAME = "filename"

        fun newInstance(imageInfo: ImageInfo, fileName: String?): ImagePreviewDialog {
            val dialog = ImagePreviewDialog()
            val args = Bundle()
            args.putSerializable(IMAGE_INFO, imageInfo)
            if (fileName != null) {
                args.putString(FILE_NAME, fileName)
            }
            dialog.arguments = args
            return dialog
        }

    }
}
