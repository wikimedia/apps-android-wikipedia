package org.wikipedia.views

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Animatable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_image_preview.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.analytics.LinkPreviewFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.page.PageClientFactory
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.GalleryThumbnailScrollView
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewOverlayView
import org.wikipedia.settings.Prefs.isImageDownloadEnabled
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L


class ImagePreviewDialog : ExtendedBottomSheetDialogFragment(), DialogInterface.OnDismissListener {

    private var navigateSuccess = false


    private var historyEntry: HistoryEntry? = null
    private var pageTitle: PageTitle? = null
    private var location: Location? = null
    private var funnel: LinkPreviewFunnel? = null
    private val disposables = CompositeDisposable()

    private val menuListener = PopupMenu.OnMenuItemClickListener { item ->
        val callback = callback()
        when (item.itemId) {
            R.id.menu_link_preview_add_to_list -> {
                callback?.onLinkPreviewAddToList(pageTitle!!)
                return@OnMenuItemClickListener true
            }
            R.id.menu_link_preview_share_page -> {
                callback?.onLinkPreviewShareLink(pageTitle!!)
                return@OnMenuItemClickListener true
            }
            R.id.menu_link_preview_copy_link -> {
                callback?.onLinkPreviewCopyLink(pageTitle!!)
                dismiss()
                return@OnMenuItemClickListener true
            }
            else -> {
            }
        }
        false
    }

    private val galleryViewListener = GalleryThumbnailScrollView.GalleryViewListener { imageName ->
        startActivityForResult(GalleryActivity.newIntent(requireContext(), pageTitle, imageName,
                pageTitle!!.wikiSite, GalleryFunnel.SOURCE_LINK_PREVIEW),
                Constants.ACTIVITY_REQUEST_GALLERY)
    }

    private val goToPageListener = { v: View -> goToLinkedPage(false) }

    interface Callback {
        fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean)
        fun onLinkPreviewCopyLink(title: PageTitle)
        fun onLinkPreviewAddToList(title: PageTitle)
        fun onLinkPreviewShareLink(title: PageTitle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val app = WikipediaApp.getInstance()
        historyEntry = arguments!!.getParcelable("entry")
        pageTitle = historyEntry!!.title
        location = arguments!!.getParcelable("location")

        val rootView = inflater.inflate(R.layout.dialog_image_preview, container)

        setConditionalLayoutDirection(rootView, pageTitle!!.wikiSite.languageCode())

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        overflowButton!!.setOnClickListener { v: View ->
            /* val popupMenu = PopupMenu(requireActivity(), overflowButton!!)
            popupMenu.inflate(R.menu.menu_link_preview)
            popupMenu.setOnMenuItemClickListener(menuListener)
            popupMenu.show() */
        }

        // show the progress bar while we load content...
        progressBar!!.visibility = View.VISIBLE

        loadContent()
    }

    fun goToLinkedPage(inNewTab: Boolean) {
        navigateSuccess = true
        funnel!!.logNavigate()
        if (dialog != null) {
            dialog.dismiss()
        }
        loadPage(pageTitle, historyEntry, inNewTab)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        disposables.clear()
        toolbarView!!.setOnClickListener(null)
        overflowButton!!.setOnClickListener(null)

        super.onDestroyView()
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


    private fun loadContent() {
        disposables.add(PageClientFactory.create(pageTitle!!.wikiSite, pageTitle!!.namespace())
                .summary<PageSummary>(pageTitle!!.wikiSite, pageTitle!!.prefixedText, historyEntry!!.referrer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ summary ->
                    //funnel!!.setPageId(summary.getPageId())
                    pageTitle!!.thumbUrl = summary.thumbnailUrl
                    // TODO: Remove this logic once Parsoid starts supporting language variants.
                    if (pageTitle!!.wikiSite.languageCode() == pageTitle!!.wikiSite.subdomain()) {
                        titleText!!.text = StringUtil.fromHtml(summary.displayTitle)
                    } else {
                        titleText!!.text = StringUtil.fromHtml(pageTitle!!.displayText)
                    }

                    // TODO: remove after the restbase endpoint supports ZH variants
                    pageTitle!!.setConvertedText(summary.convertedTitle)
                    loadImage(pageTitle!!.thumbUrl)
                }, { caught ->
                    L.e(caught)
                    titleText!!.text = StringUtil.fromHtml(pageTitle!!.displayText)
                    showError(caught)
                }))
    }

    private fun loadGallery() {
        if (isImageDownloadEnabled()) {
            disposables.add(ServiceFactory.getRest(pageTitle!!.wikiSite).getMedia(pageTitle!!.convertedText)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ gallery ->
                    }, { caught ->
                        // ignore errors
                        L.w("Failed to fetch gallery collection.", caught)
                    }))
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

    private fun goToExternalMapsApp() {
        if (location != null) {
            dismiss()
            GeoUtil.sendGeoIntent(requireActivity(), location!!, pageTitle!!.displayText)
        }
    }

    private fun loadPage(title: PageTitle?, entry: HistoryEntry?, inNewTab: Boolean) {
        val callback = callback()
        callback?.onLinkPreviewLoadPage(title!!, entry!!, inNewTab)
    }

    private inner class OverlayViewCallback : LinkPreviewOverlayView.Callback {
        override fun onPrimaryClick() {
            goToLinkedPage(false)
        }

        override fun onSecondaryClick() {
            goToLinkedPage(true)
        }

        override fun onTertiaryClick() {
            goToExternalMapsApp()
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
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
