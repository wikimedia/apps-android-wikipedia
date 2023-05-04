package org.wikipedia.suggestededits

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.icu.text.ListFormatter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecsItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.FaceAndColorDetectImageView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewAnimations
import org.wikipedia.watchlist.WatchlistViewModel
import java.util.*

class SuggestedEditsImageRecsFragment : SuggestedEditsItemFragment(), SuggestedEditsImageRecsDialog.Callback {
    private var _binding: FragmentSuggestedEditsImageRecsItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuggestedEditsImageRecsFragmentViewModel by viewModels()

    private var publishing = false
    private var publishSuccess = false
    // private var recommendation: ImageRecommendationResponse? = null

    private var infoClicked = false
    private var detailsClicked = false
    private var scrolled = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<CoordinatorLayout>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsImageRecsItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding.cardItemErrorView.backClickListener = OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = OnClickListener {
            binding.cardItemProgressBar.visibility = VISIBLE
            binding.cardItemErrorView.visibility = GONE
            getNextItem()
        }
        binding.cardItemErrorView.nextClickListener = OnClickListener { callback().nextPage(this) }

        val transparency = 0xd8000000

        binding.publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.publishOverlayContainer.visibility = GONE

        binding.imageCard.elevation = 0f
        binding.imageCard.strokeColor = ResourceUtil.getThemedColor(requireContext(), R.attr.border_color)
        binding.imageCard.strokeWidth = DimenUtil.roundedDpToPx(0.5f)

        binding.acceptButton.setOnClickListener {
            doPublish(0, emptyList())
        }

        binding.rejectButton.setOnClickListener {
            //ImageRecsDialog.newInstance(ImageRecommendationsFunnel.RESPONSE_REJECT).show(childFragmentManager, null)
        }

        binding.notSureButton.setOnClickListener {
            //ImageRecsDialog.newInstance(ImageRecommendationsFunnel.RESPONSE_NOT_SURE).show(childFragmentManager, null)
        }

        binding.imageCard.setOnClickListener {
            /*
            recommendation?.let {
                startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + it.image, WikiSite(Service.COMMONS_URL)), false))
                detailsClicked = true
            }
             */
        }

        binding.imageCard.setOnLongClickListener {
            if (ImageZoomHelper.isZooming) {
                // Dispatch a fake CANCEL event to the container view, so that the long-press ripple is cancelled.
                binding.imageCard.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0))
            }
            false
        }

        binding.articleContentContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            scrolled = true
        })

        binding.readMoreButton.setOnClickListener {
            /*
            recommendation?.let {
                val title = PageTitle(it.pageTitle, WikipediaApp.instance.wikiSite)
                startActivity(PageActivity.newIntentForNewTab(requireActivity(), HistoryEntry(title, HistoryEntry.SOURCE_SUGGESTED_EDITS), title))
            }
             */
        }

        ImageZoomHelper.setViewZoomable(binding.imageView)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetCoordinatorLayout).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is SuggestedEditsImageRecsFragmentViewModel.UiState.Loading -> onLoading()
                        is SuggestedEditsImageRecsFragmentViewModel.UiState.Success -> onLoadSuccess()
                        is SuggestedEditsImageRecsFragmentViewModel.UiState.Error -> onError(it.throwable)
                    }
                }
            }
        }

        getNextItem()
        updateContents(null)
    }

    override fun onStart() {
        super.onStart()
        callback().updateActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onLoading() {

    }

    private fun onLoadSuccess() {

    }

    private fun onError(throwable: Throwable) {

    }

    private fun maybeShowTooltipSequence() {
        binding.root.post {
            if (!isResumed || !isAdded) {
                return@post
            }
            /*
            if (Prefs.shouldShowImageRecsOnboarding()) {
                Prefs.setShowImageRecsOnboarding(false)
                val balloon = FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendations_tooltip1), autoDismiss = true, showDismissButton = true)
                balloon.showAlignBottom(binding.articleTitlePlaceholder)
                balloon.relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendations_tooltip2), autoDismiss = true, showDismissButton = true), binding.instructionText)
                    .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendations_tooltip3), autoDismiss = true, showDismissButton = true), binding.acceptButton)
            }
             */
        }
    }

    private fun getNextItem() {
        /*
        if (recommendation != null) {
            return
        }
        disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingImage(WikipediaApp.instance.appOrSystemLanguageCode)
            .subscribeOn(Schedulers.io())
            .flatMap {
                recommendation = it
                ServiceFactory.get(WikipediaApp.instance.wikiSite).getInfoByPageId(recommendation!!.pageId.toString())
            }
            .flatMap {
                recommendation!!.pageTitle = it.query()!!.firstPage()!!.displayTitle(WikipediaApp.instance.appOrSystemLanguageCode)
                if (recommendation!!.pageTitle.isEmpty()) {
                    throw ThrowableUtil.EmptyException()
                }
                ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getSummary(null, recommendation!!.pageTitle).subscribeOn(Schedulers.io())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .retry(10)
            .subscribe({ summary ->
                updateContents(summary)
            }, { this.setErrorState(it) }))
         */
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        recommendation = null
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.articleContentContainer.visibility = GONE
        binding.imageSuggestionContainer.visibility = GONE
    }

    private fun updateContents(summary: PageSummary?) {
        binding.cardItemErrorView.visibility = GONE
        binding.articleContentContainer.visibility = if (recommendation != null) VISIBLE else GONE
        binding.imageSuggestionContainer.visibility = GONE
        binding.readMoreButton.visibility = GONE
        binding.cardItemProgressBar.visibility = VISIBLE
        if (recommendation == null || summary == null) {
            return
        }

        disposables.add((if (siteInfoList == null)
            ServiceFactory.get(WikiSite(Service.COMMONS_URL)).siteMatrix
                .subscribeOn(Schedulers.io())
                .map {
                    siteInfoList = SiteMatrix.getSites(it)
                    siteInfoList!!
                }
        else Observable.just(siteInfoList!!))
            .flatMap {
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo("File:" + recommendation!!.image, WikipediaApp.instance.appOrSystemLanguageCode)
                    .subscribeOn(Schedulers.io())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .retry(5)
            .subscribe({ response ->
                binding.cardItemProgressBar.visibility = GONE

                val imageInfo = response.query()!!.firstPage()!!.imageInfo()!!

                binding.articleTitle.text = StringUtil.fromHtml(summary.displayTitle)
                binding.articleDescription.text = summary.description
                binding.articleExtract.text = StringUtil.fromHtml(summary.extractHtml).trim()
                binding.readMoreButton.visibility = VISIBLE

                binding.imageView.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(imageInfo.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)),
                    roundedCorners = false, cropped = false, listener = object : FaceAndColorDetectImageView.OnImageLoadListener {
                        override fun onImageLoaded(palette: Palette, bmpWidth: Int, bmpHeight: Int) {
                            if (isAdded) {
                                var color1 = palette.getLightVibrantColor(ContextCompat.getColor(requireContext(), R.color.base70))
                                var color2 = palette.getLightMutedColor(ContextCompat.getColor(requireContext(), R.color.base30))
                                if (WikipediaApp.instance.currentTheme.isDark) {
                                    color1 = ResourceUtil.darkenColor(color1)
                                    color2 = ResourceUtil.darkenColor(color2)
                                }

                                val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, arrayOf(color1, color2).toIntArray())
                                binding.imageViewContainer.background = gradientDrawable

                                val params = binding.imageInfoButton.layoutParams as FrameLayout.LayoutParams
                                val containerAspect = binding.imageViewContainer.width.toFloat() / binding.imageViewContainer.height.toFloat()
                                val bmpAspect = bmpWidth.toFloat() / bmpHeight.toFloat()

                                if (bmpAspect > containerAspect) {
                                    params.marginEnd = DimenUtil.roundedDpToPx(8f)
                                } else {
                                    val width = binding.imageViewContainer.height.toFloat() * bmpAspect
                                    params.marginEnd = DimenUtil.roundedDpToPx(8f) + (binding.imageViewContainer.width / 2 - width.toInt() / 2)
                                }
                                binding.imageInfoButton.layoutParams = params
                            }
                        }

                        override fun onImageFailed() {}
                    })
                binding.imageCaptionText.text = if (imageInfo.metadata == null) null else StringUtil.removeHTMLTags(imageInfo.metadata!!.imageDescription())

                binding.articleScrollSpacer.post {
                    if (isAdded) {
                        binding.articleScrollSpacer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, bottomSheetBehavior.peekHeight)
                        // Collapse bottom sheet if the article title is not visible when loaded
                        binding.suggestedEditsItemRootView.doViewsOverlap(binding.articleTitle, binding.bottomSheetCoordinatorLayout).run {
                            if (this) {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }
                        }
                    }
                }

                val arr = imageInfo.commonsUrl.split('/')
                binding.imageFileNameText.text = StringUtil.removeUnderscores(UriUtil.decodeURL(arr[arr.size - 1]))
                binding.imageSuggestionReason.text = StringUtil.fromHtml(getString(R.string.image_recommendations_task_suggestion_reason, getSuggestionReason()))

                ViewAnimations.fadeIn(binding.imageSuggestionContainer)

                maybeShowTooltipSequence()
            }, { setErrorState(it) }))

        callback().updateActionButton()
    }

    override fun publish() {
        // the "Publish" button in our case is actually the "skip" button.
        callback().nextPage(this)
    }

    override fun onDialogSubmit(response: Int, selectedItems: List<Int>) {
        doPublish(response, selectedItems)
    }

    private fun doPublish(response: Int, reasons: List<Int>) {
        if (publishing || publishSuccess || recommendation == null) {
            return
        }

        // -- point of no return --

        publishing = true
        publishSuccess = false

        binding.publishProgressCheck.visibility = GONE
        binding.publishOverlayContainer.visibility = VISIBLE
        binding.publishProgressBar.visibility = VISIBLE

        publishSuccess = true
        onPublishSuccess()
    }

    private fun onPublishSuccess() {
        val waitUntilNextMillis = 1500L

        val checkDelayMillis = 700L
        val checkAnimationDuration = 300L

        var progressCount = 0
        binding.publishProgressBar.post(object : Runnable {
            override fun run() {
                if (isAdded) {
                    if (binding.publishProgressBar.progress >= 100) {
                        binding.publishProgressCheck.alpha = 0f
                        binding.publishProgressCheck.visibility = VISIBLE
                        binding.publishProgressCheck.animate()
                            .alpha(1f)
                            .withEndAction {
                                binding.publishProgressBar.postDelayed({
                                    if (isAdded) {
                                        binding.publishOverlayContainer.visibility = GONE
                                        callback().nextPage(this@SuggestedEditsImageRecsFragment)
                                        callback().logSuccess()
                                    }
                                }, waitUntilNextMillis + checkDelayMillis)
                            }
                            .duration = checkAnimationDuration
                    } else {
                        binding.publishProgressBar.progress = ++progressCount * 3
                        binding.publishProgressBar.post(this)
                    }
                }
            }
        })
    }

    override fun publishEnabled(): Boolean {
        return true
    }

    override fun publishOutlined(): Boolean {
        return false
    }

    private fun getSuggestionReason(): String {
        /*
        val hasWikidata = recommendation!!.foundOnWikis.contains("wd")
        val langWikis = recommendation!!.foundOnWikis
            .sortedBy { WikipediaApp.instance.languageState.getLanguageCodeIndex(it) }
            .take(3)
            .mapNotNull { getCanonicalName(it) }

        if (langWikis.isNotEmpty()) {
            return getString(R.string.image_recommendations_task_suggestion_reason_wikilist,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ListFormatter.getInstance().format(langWikis)
                } else {
                    langWikis.joinToString(separator = ", ")
                })
        } else if (hasWikidata) {
            return getString(R.string.image_recommendations_task_suggestion_reason_wikidata)
        }
        return getString(R.string.image_recommendations_task_suggestion_reason_commons)
         */
        return ""
    }

    private fun getFunnelReason(): String {
        /*
        val hasWikidata = recommendation!!.foundOnWikis.contains("wd")
        val langWikis = recommendation!!.foundOnWikis.filter { it != "wd" && it != "com" && it != "species" }
        return when {
            langWikis.isNotEmpty() -> {
                "wikipedia"
            }
            hasWikidata -> {
                "wikidata"
            }
            else -> "commons"
        }
         */
        return ""
    }

    private fun getCanonicalName(code: String): String? {
        var canonicalName = siteInfoList?.find { it.code == code }?.localname
        if (canonicalName.isNullOrEmpty()) {
            canonicalName = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(code)
        }
        return canonicalName
    }

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }

    fun onInfoClicked() {
        infoClicked = true
    }

    companion object {
        private val SUPPORTED_LANGUAGES = arrayOf("ar", "arz", "bn", "cs", "de", "en", "es", "eu", "fa", "fr", "he", "hu", "hy", "it", "ko", "pl", "pt", "ru", "sr", "sv", "tr", "uk", "vi")
        private var siteInfoList: List<SiteMatrix.SiteInfo>? = null

        fun isFeatureEnabled(): Boolean {
            return AccountUtil.isLoggedIn &&
                    SUPPORTED_LANGUAGES.any { it == WikipediaApp.instance.appOrSystemLanguageCode }
        }

        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageRecsFragment()
        }
    }
}
