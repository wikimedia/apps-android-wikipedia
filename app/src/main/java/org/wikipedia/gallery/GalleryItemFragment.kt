package org.wikipedia.gallery

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.MediaController
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentGalleryItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewUtil

class GalleryItemFragment : Fragment(), RequestListener<Drawable?> {
    interface Callback {
        fun onDownload(item: GalleryItemFragment)
        fun onShare(item: GalleryItemFragment, bitmap: Bitmap?, subject: String, title: PageTitle)
    }

    private var _binding: FragmentGalleryItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaListItem: MediaListItem
    private val disposables = CompositeDisposable()
    private var mediaController: MediaController? = null
    private var pageTitle: PageTitle? = null
    var imageTitle: PageTitle? = null
    var mediaPage: MwQueryPage? = null
    val mediaInfo get() = mediaPage?.imageInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaListItem = requireArguments().getSerializable(ARG_GALLERY_ITEM) as MediaListItem
        pageTitle = requireArguments().getParcelable(ARG_PAGETITLE)
        if (pageTitle == null) {
            pageTitle = PageTitle(mediaListItem.title, WikiSite(Service.COMMONS_URL))
        }
        imageTitle = PageTitle(Namespace.FILE.toLegacyString(), StringUtil.removeNamespace(mediaListItem.title),
            pageTitle!!.wikiSite
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGalleryItemBinding.inflate(inflater, container, false)

        binding.image.setOnClickListener {
            if (!isAdded) {
                return@setOnClickListener
            }
            (requireActivity() as GalleryActivity).toggleControls()
        }
        binding.image.setOnMatrixChangeListener {
            if (!isAdded) {
                return@setOnMatrixChangeListener
            }
            (requireActivity() as GalleryActivity).setViewPagerEnabled(binding.image.scale <= 1f)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        loadMedia()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.image.setOnClickListener(null)
        binding.videoThumbnail.setOnClickListener(null)
        _binding = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        mediaController?.let {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
            }
            it.hide()
        }
    }

    private fun updateProgressBar(visible: Boolean) {
        binding.progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (!isAdded) {
            return
        }
        menu.findItem(R.id.menu_gallery_visit_image_page).isEnabled = mediaInfo != null
        menu.findItem(R.id.menu_gallery_share).isEnabled = mediaInfo != null &&
                mediaInfo!!.thumbUrl.isNotEmpty() && binding.image.drawable != null
        menu.findItem(R.id.menu_gallery_save).isEnabled = mediaInfo != null &&
                mediaInfo!!.thumbUrl.isNotEmpty() && binding.image.drawable != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_gallery_visit_image_page -> {
                if (mediaInfo != null && imageTitle != null) {
                    startActivity(FilePageActivity.newIntent(requireContext(), imageTitle!!))
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
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleImageSaveRequest() {
        if (!PermissionUtil.hasWriteExternalStoragePermission(requireActivity())) {
            requestWriteExternalStoragePermission()
        } else {
            saveImage()
        }
    }

    private fun requestWriteExternalStoragePermission() {
        PermissionUtil.requestWriteStorageRuntimePermissions(this,
            Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
        )
    }

    private fun loadMedia() {
        if (pageTitle == null) {
            return
        }
        updateProgressBar(true)
        disposables.add(getMediaInfoDisposable(mediaListItem.title, WikipediaApp.getInstance().appOrSystemLanguageCode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                updateProgressBar(false)
                requireActivity().invalidateOptionsMenu()
                (requireActivity() as GalleryActivity).layOutGalleryDescription()
            }
            .subscribe({ response ->
                mediaPage = response.query()!!.firstPage()
                if (FileUtil.isVideo(mediaListItem.type)) {
                    loadVideo()
                } else {
                    loadImage(ImageUrlUtil.getUrlForPreferredSize(mediaInfo!!.thumbUrl,
                        Constants.PREFERRED_GALLERY_IMAGE_SIZE))
                }
            }) { throwable ->
                FeedbackUtil.showMessage(requireActivity(), R.string.gallery_error_draw_failed)
                L.d(throwable)
            })
    }

    private fun getMediaInfoDisposable(title: String, lang: String): Observable<MwQueryResponse> {
        return if (FileUtil.isVideo(mediaListItem.type)) {
            ServiceFactory.get(if (mediaListItem.isInCommons) WikiSite(Service.COMMONS_URL)
            else pageTitle!!.wikiSite).getVideoInfo(title, lang)
        } else {
            ServiceFactory.get(if (mediaListItem.isInCommons) WikiSite(Service.COMMONS_URL)
            else pageTitle!!.wikiSite).getImageInfo(title, lang)
        }
    }

    private val videoThumbnailClickListener: View.OnClickListener = object : View.OnClickListener {
        private var loading = false
        override fun onClick(v: View) {
            if (loading || mediaInfo?.bestDerivative == null) {
                return
            }
            loading = true
            L.d("Loading video from url: " + mediaInfo!!.bestDerivative!!.src)
            binding.videoView.visibility = View.VISIBLE
            mediaController = MediaController(requireActivity())
            if (!DeviceUtil.isNavigationBarShowing) {
                mediaController?.setPadding(0, 0, 0,
                    DimenUtil.dpToPx(DimenUtil.getNavigationBarHeight(requireContext())).toInt())
            }
            updateProgressBar(true)
            binding.videoView.setMediaController(mediaController)
            binding.videoView.setOnPreparedListener {
                updateProgressBar(false)
                // ...update the parent activity, which will trigger us to start playing!
                (requireActivity() as GalleryActivity).layOutGalleryDescription()
                // hide the video thumbnail, since we're about to start playback
                binding.videoThumbnail.visibility = View.GONE
                binding.videoPlayButton.visibility = View.GONE
                // and start!
                binding.videoView.start()
                loading = false
            }
            binding.videoView.setOnErrorListener { _, _, _ ->
                updateProgressBar(false)
                FeedbackUtil.showMessage(activity!!, R.string.gallery_error_video_failed)
                binding.videoView.visibility = View.GONE
                binding.videoThumbnail.visibility = View.VISIBLE
                binding.videoPlayButton.visibility = View.VISIBLE
                loading = false
                true
            }
            binding.videoView.setVideoURI(Uri.parse(mediaInfo!!.bestDerivative!!.src))
        }
    }

    private fun loadVideo() {
        binding.videoContainer.visibility = View.VISIBLE
        binding.videoPlayButton.visibility = View.VISIBLE
        binding.videoView.visibility = View.GONE
        if (mediaInfo == null || mediaInfo!!.thumbUrl.isEmpty()) {
            binding.videoThumbnail.visibility = View.GONE
        } else {
            // show the video thumbnail while the video loads...
            binding.videoThumbnail.visibility = View.VISIBLE
            ViewUtil.loadImage(binding.videoThumbnail, mediaInfo!!.thumbUrl)
        }
        binding.videoThumbnail.setOnClickListener(videoThumbnailClickListener)
    }

    private fun loadImage(url: String) {
        binding.image.visibility = View.INVISIBLE
        L.v("Loading image from url: $url")
        updateProgressBar(true)
        ViewUtil.loadImage(binding.image, url, roundedCorners = false, largeRoundedSize = false, force = true, listener = this)
        // TODO: show error if loading failed.
    }

    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
        (requireActivity() as GalleryActivity).onMediaLoaded()
        return false
    }

    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource,
        isFirstResource: Boolean): Boolean {
        binding.image.visibility = View.VISIBLE
        (requireActivity() as GalleryActivity).onMediaLoaded()
        return false
    }

    private fun shareImage() {
        mediaInfo?.let {
            object : ImagePipelineBitmapGetter(ImageUrlUtil.getUrlForPreferredSize(it.thumbUrl,
                    Constants.PREFERRED_GALLERY_IMAGE_SIZE)) {
                override fun onSuccess(bitmap: Bitmap?) {
                    if (!isAdded) {
                        return
                    }
                    shareSubject?.let { subject ->
                        imageTitle?.let { title ->
                            callback()?.onShare(this@GalleryItemFragment, bitmap, subject, title)
                        }
                    }
                }
            }[requireContext()]
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (PermissionUtil.isPermitted(grantResults)) {
                saveImage()
            } else {
                L.e("Write permission was denied by user")
                FeedbackUtil.showMessage(requireActivity(), R.string.gallery_save_image_write_permission_rationale)
            }
        } else {
            throw RuntimeException("unexpected permission request code $requestCode")
        }
    }

    private val shareSubject get() = pageTitle?.displayText

    private fun saveImage() {
        mediaInfo?.let { callback()?.onDownload(this) }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val ARG_PAGETITLE = "pageTitle"
        private const val ARG_GALLERY_ITEM = "galleryItem"

        fun newInstance(pageTitle: PageTitle?, item: MediaListItem): GalleryItemFragment {
            return GalleryItemFragment().apply {
                arguments = bundleOf(ARG_PAGETITLE to pageTitle, ARG_GALLERY_ITEM to item)
            }
        }
    }
}
