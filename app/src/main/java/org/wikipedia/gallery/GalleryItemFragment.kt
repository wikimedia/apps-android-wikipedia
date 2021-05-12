package org.wikipedia.gallery

import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.commons.FilePageActivity.Companion.newIntent
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil.isNavigationBarShowing
import org.wikipedia.util.DimenUtil.dpToPx
import org.wikipedia.util.DimenUtil.getNavigationBarHeight
import org.wikipedia.util.FeedbackUtil.showMessage
import org.wikipedia.util.FileUtil.isVideo
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize
import org.wikipedia.util.PermissionUtil.hasWriteExternalStoragePermission
import org.wikipedia.util.PermissionUtil.isPermitted
import org.wikipedia.util.PermissionUtil.requestWriteStorageRuntimePermissions
import org.wikipedia.util.StringUtil.removeNamespace
import org.wikipedia.util.log.L.d
import org.wikipedia.util.log.L.e
import org.wikipedia.util.log.L.v
import org.wikipedia.views.ViewUtil.loadImage

class GalleryItemFragment : Fragment(), RequestListener<Drawable?> {
    interface Callback {
        fun onDownload(item: GalleryItemFragment)
        fun onShare(item: GalleryItemFragment, bitmap: Bitmap?, subject: String, title: PageTitle)
    }

    @JvmField
    @BindView(R.id.gallery_item_progress_bar)
    var progressBar: ProgressBar? = null

    @JvmField
    @BindView(R.id.gallery_video_container)
    var videoContainer: View? = null

    @JvmField
    @BindView(R.id.gallery_video)
    var videoView: VideoView? = null

    @JvmField
    @BindView(R.id.gallery_video_thumbnail)
    var videoThumbnail: ImageView? = null

    @JvmField
    @BindView(R.id.gallery_video_play_button)
    var videoPlayButton: View? = null

    @JvmField
    @BindView(R.id.gallery_image)
    var imageView: PhotoView? = null
    private var unbinder: Unbinder? = null
    private val disposables = CompositeDisposable()
    private var mediaController: MediaController? = null
    private var pageTitle: PageTitle? = null
    private var mediaListItem: MediaListItem? = null
    var imageTitle: PageTitle? = null
        private set
    val mediaInfo: ImageInfo?
        get() = if (mediaPage != null) mediaPage!!.imageInfo() else null
    var mediaPage: MwQueryPage? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaListItem = requireArguments().getSerializable(ARG_GALLERY_ITEM) as MediaListItem?
        pageTitle = requireArguments().getParcelable(ARG_PAGETITLE)
        if (pageTitle == null) {
            pageTitle = PageTitle(mediaListItem!!.title, WikiSite(Service.COMMONS_URL))
        }
        imageTitle = PageTitle(
            Namespace.FILE.toLegacyString(),
            removeNamespace(mediaListItem!!.title),
            pageTitle!!.wikiSite
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_gallery_item, container, false)
        unbinder = ButterKnife.bind(this, rootView)
        imageView!!.setOnClickListener { v: View? ->
            if (isAdded) {
                (requireActivity() as GalleryActivity).toggleControls()
            }
        }
        imageView!!.setOnMatrixChangeListener { rect: RectF? ->
            if (!isAdded || imageView == null) {
                return@setOnMatrixChangeListener
            }
            (requireActivity() as GalleryActivity).setViewPagerEnabled(imageView!!.scale <= 1f)
        }
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        loadMedia()
    }

    override fun onDestroyView() {
        disposables.clear()
        imageView!!.setOnClickListener(null)
        videoThumbnail!!.setOnClickListener(null)
        if (unbinder != null) {
            unbinder!!.unbind()
            unbinder = null
        }
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        if (mediaController != null) {
            if (videoView!!.isPlaying) {
                videoView!!.pause()
            }
            mediaController!!.hide()
        }
    }

    private fun updateProgressBar(visible: Boolean) {
        progressBar!!.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (!isAdded) {
            return
        }
        menu.findItem(R.id.menu_gallery_visit_image_page).isEnabled = mediaInfo != null
        menu.findItem(R.id.menu_gallery_share).isEnabled =
            mediaInfo != null && !TextUtils.isEmpty(mediaInfo!!.thumbUrl) && imageView!!.drawable != null
        menu.findItem(R.id.menu_gallery_save).isEnabled =
            mediaInfo != null && !TextUtils.isEmpty(mediaInfo!!.thumbUrl) && imageView!!.drawable != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_gallery_visit_image_page -> {
                if (mediaInfo != null && imageTitle != null) {
                    startActivity(newIntent(requireContext(), imageTitle!!))
                }
                return true
            }
            R.id.menu_gallery_save -> {
                handleImageSaveRequest()
                return true
            }
            R.id.menu_gallery_share -> {
                shareImage()
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleImageSaveRequest() {
        if (!hasWriteExternalStoragePermission(requireActivity())) {
            requestWriteExternalStoragePermission()
        } else {
            saveImage()
        }
    }

    private fun requestWriteExternalStoragePermission() {
        requestWriteStorageRuntimePermissions(
            this,
            Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
        )
    }

    /**
     * Load the actual media associated with our gallery item into the UI.
     */
    private fun loadMedia() {
        if (pageTitle == null || mediaListItem == null) {
            return
        }
        updateProgressBar(true)
        disposables.add(getMediaInfoDisposable(
            mediaListItem!!.title,
            WikipediaApp.getInstance().appOrSystemLanguageCode
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                updateProgressBar(false)
                requireActivity().invalidateOptionsMenu()
                (requireActivity() as GalleryActivity).layOutGalleryDescription()
            }
            .subscribe({ response: MwQueryResponse ->
                mediaPage = response.query()!!.firstPage()
                if (isVideo(mediaListItem!!.type)) {
                    loadVideo()
                } else {
                    loadImage(
                        getUrlForPreferredSize(
                            mediaInfo!!.thumbUrl,
                            Constants.PREFERRED_GALLERY_IMAGE_SIZE
                        )
                    )
                }
            }) { throwable: Throwable? ->
                showMessage(activity!!, R.string.gallery_error_draw_failed)
                d(throwable)
            })
    }

    private fun getMediaInfoDisposable(title: String, lang: String): Observable<MwQueryResponse> {
        return if (isVideo(mediaListItem!!.type)) {
            ServiceFactory.get(if (mediaListItem!!.isInCommons) WikiSite(Service.COMMONS_URL) else pageTitle!!.wikiSite)
                .getVideoInfo(title, lang)
        } else {
            ServiceFactory.get(if (mediaListItem!!.isInCommons) WikiSite(Service.COMMONS_URL) else pageTitle!!.wikiSite)
                .getImageInfo(title, lang)
        }
    }

    private val videoThumbnailClickListener: View.OnClickListener = object : View.OnClickListener {
        private var loading = false
        override fun onClick(v: View) {
            if (loading || mediaInfo == null || mediaInfo!!.bestDerivative == null) {
                return
            }
            loading = true
            d("Loading video from url: " + mediaInfo!!.bestDerivative!!.src)
            videoView!!.visibility = View.VISIBLE
            mediaController = MediaController(requireActivity())
            if (!isNavigationBarShowing) {
                mediaController!!.setPadding(
                    0, 0, 0, dpToPx(getNavigationBarHeight(requireContext()))
                        .toInt()
                )
            }
            updateProgressBar(true)
            videoView!!.setMediaController(mediaController)
            videoView!!.setOnPreparedListener { mp: MediaPlayer? ->
                updateProgressBar(false)
                // ...update the parent activity, which will trigger us to start playing!
                (requireActivity() as GalleryActivity).layOutGalleryDescription()
                // hide the video thumbnail, since we're about to start playback
                videoThumbnail!!.visibility = View.GONE
                videoPlayButton!!.visibility = View.GONE
                // and start!
                videoView!!.start()
                loading = false
            }
            videoView!!.setOnErrorListener { mp: MediaPlayer?, what: Int, extra: Int ->
                updateProgressBar(false)
                showMessage(
                    activity!!,
                    R.string.gallery_error_video_failed
                )
                videoView!!.visibility = View.GONE
                videoThumbnail!!.visibility = View.VISIBLE
                videoPlayButton!!.visibility = View.VISIBLE
                loading = false
                true
            }
            videoView!!.setVideoURI(Uri.parse(mediaInfo!!.bestDerivative!!.src))
        }
    }

    private fun loadVideo() {
        videoContainer!!.visibility = View.VISIBLE
        videoPlayButton!!.visibility = View.VISIBLE
        videoView!!.visibility = View.GONE
        if (mediaInfo == null || TextUtils.isEmpty(mediaInfo!!.thumbUrl)) {
            videoThumbnail!!.visibility = View.GONE
        } else {
            // show the video thumbnail while the video loads...
            videoThumbnail!!.visibility = View.VISIBLE
            loadImage(videoThumbnail!!, mediaInfo!!.thumbUrl)
        }
        videoThumbnail!!.setOnClickListener(videoThumbnailClickListener)
    }

    private fun loadImage(url: String) {
        imageView!!.visibility = View.INVISIBLE
        v("Loading image from url: $url")
        updateProgressBar(true)
        loadImage(imageView!!, url, false, false, true, this)
        // TODO: show error if loading failed.
    }

    override fun onLoadFailed(
        e: GlideException?,
        model: Any,
        target: Target<Drawable?>,
        isFirstResource: Boolean
    ): Boolean {
        (requireActivity() as GalleryActivity).onMediaLoaded()
        return false
    }

    override fun onResourceReady(
        resource: Drawable?,
        model: Any,
        target: Target<Drawable?>,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        imageView!!.visibility = View.VISIBLE
        (requireActivity() as GalleryActivity).onMediaLoaded()
        return false
    }

    private fun shareImage() {
        if (mediaInfo == null) {
            return
        }
        object : ImagePipelineBitmapGetter(
            getUrlForPreferredSize(
                mediaInfo!!.thumbUrl,
                Constants.PREFERRED_GALLERY_IMAGE_SIZE
            )
        ) {
            override fun onSuccess(bitmap: Bitmap?) {
                if (!isAdded) {
                    return
                }
                if (callback() != null && shareSubject != null && imageTitle != null) {
                    callback()!!.onShare(
                        this@GalleryItemFragment,
                        bitmap,
                        shareSubject!!,
                        imageTitle!!
                    )
                }
            }
        }[requireContext()]
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (isPermitted(grantResults)) {
                saveImage()
            } else {
                e("Write permission was denied by user")
                showMessage(
                    activity!!,
                    R.string.gallery_save_image_write_permission_rationale
                )
            }
        } else {
            throw RuntimeException("unexpected permission request code $requestCode")
        }
    }

    private val shareSubject: String?
        private get() = if (pageTitle != null) pageTitle!!.displayText else null

    private fun saveImage() {
        if (mediaInfo != null && callback() != null) {
            callback()!!.onDownload(this)
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    companion object {
        private const val ARG_PAGETITLE = "pageTitle"
        private const val ARG_GALLERY_ITEM = "galleryItem"

        @JvmStatic
        fun newInstance(pageTitle: PageTitle?, item: MediaListItem): GalleryItemFragment {
            val f = GalleryItemFragment()
            val args = Bundle()
            args.putParcelable(ARG_PAGETITLE, pageTitle)
            args.putSerializable(ARG_GALLERY_ITEM, item)
            f.arguments = args
            return f
        }
    }
}
