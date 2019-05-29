package org.wikipedia.views

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Animatable
import android.location.Location
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
import com.facebook.imagepipeline.image.ImageInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_image_preview.*
import org.wikipedia.BuildConfig
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.gallery.ExtMetadata
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L


class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {


    private var location: Location? = null
    private val disposables = CompositeDisposable()

    private val menuListener = PopupMenu.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.menu_link_preview_add_to_list -> {
                return@OnMenuItemClickListener true
            }
            R.id.menu_link_preview_share_page -> {
                return@OnMenuItemClickListener true
            }
            R.id.menu_link_preview_copy_link -> {
                return@OnMenuItemClickListener true
            }

        }
        false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val app = WikipediaApp.getInstance()

        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)

        setConditionalLayoutDirection(rootView, WikipediaApp.getInstance().language().appLanguageCode)

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
        imagePageCommonsLinkContainer.setOnClickListener {
            UriUtil.visitInExternalBrowser(context,
                    Uri.parse(String.format(requireContext().getString(R.string.donate_url),
                            BuildConfig.VERSION_NAME, WikipediaApp.getInstance().language().systemLanguageCode)))
        }
        getImageDetails()
    }

    override fun onDestroyView() {
        disposables.clear()
        toolbarView!!.setOnClickListener(null)
        overflowButton!!.setOnClickListener(null)

        super.onDestroyView()
    }

    private fun getImageDetails() {
        disposables.add(ServiceFactory.get(WikipediaApp.getInstance().getWikiSite()).getMedia()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mwresponse ->
                    val page: MwQueryPage = mwresponse.query()!!.pages()!![0]
                    val metaData: ExtMetadata = page.imageInfo()!!.getMetadata()!!
                    loadImage(page.imageInfo()!!.originalUrl)
                    titleText!!.text = StringUtil.fromHtml(page.title().removePrefix("File:"))
                    addDetailPortion(getString(R.string.suggested_edits_image_caption_title), StringUtil.fromHtml(metaData.imageDescription()!!.value()).toString())
                    addDetailPortion(getString(R.string.suggested_edits_image_depicts_title), StringUtil.fromHtml(metaData.imageDescription()!!.value()).toString())
                    addDetailPortion(getString(R.string.suggested_edits_image_artist), StringUtil.fromHtml(metaData.artist()!!.value()).toString())
                    addDetailPortion(getString(R.string.suggested_edits_image_date), metaData.dateTime()!!.value())
                    addDetailPortion(getString(R.string.suggested_edits_image_medium), metaData.dateTime()!!.value())
                    addDetailPortion(getString(R.string.suggested_edits_image_source), metaData.imageDescription()!!.source())
                    addDetailPortion(getString(R.string.suggested_edits_image_licensing), metaData.licenseShortName()!!.value())
                    addDetailPortion(getString(R.string.suggested_edits_image_licensing), metaData.licenseShortName()!!.value())
                    detailsHolder.requestLayout()
                }, { this.setErrorState(it) }))
    }

    private fun addDetailPortion(@NonNull title: String, @Nullable detail: String?) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailPortionView(requireContext())
            view.setTitle(title)
            view.setDetail(detail)
            detailsHolder.addView(view)
        }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)

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
                .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                        galleryImage.setDrawBackground(true)
                    }

                    override fun onFailure(id: String?, throwable: Throwable?) {
                        L.d(throwable)
                    }
                })
                .build()
    }

    private fun showError(caught: Throwable?) {
        /* dialogContainer!!.layoutTransition = null
         dialogContainer!!.minimumHeight = 0
         progressBar!!.visibility = View.GONE
         contentContainer!!.visibility = View.GONE
         errorContainer!!.visibility = View.VISIBLE
         errorContainer!!.setError(caught)
         errorContainer!!.setCallback(this)*/
    }

    companion object {

        fun newInstance(entry: HistoryEntry, location: Location?): ImagePreviewDialog {
            val dialog = ImagePreviewDialog()
            val args = Bundle()
            args.putParcelable("entry", entry)
            if (location != null) {
                args.putParcelable("location", location)
            }
            dialog.arguments = args
            return dialog
        }

    }
}
