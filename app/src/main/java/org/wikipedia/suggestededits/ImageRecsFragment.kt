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
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.palette.graphics.Palette
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.ImageRecommendationsFunnel
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecommendationItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.restbase.ImageRecommendationResponse
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
import java.util.*

class ImageRecsFragment : SuggestedEditsItemFragment(), ImageRecsDialog.Callback {
    private var _binding: FragmentSuggestedEditsImageRecommendationItemBinding? = null
    private val binding get() = _binding!!

    var publishing: Boolean = false
    private var publishSuccess: Boolean = false
    private var recommendation: ImageRecommendationResponse? = null
    private var recommendationSequence: Int = 0

    private val funnel = ImageRecommendationsFunnel()
    private var startMillis: Long = 0
    private var buttonClickedMillis: Long = 0
    private var infoClicked: Boolean = false
    private var detailsClicked: Boolean = false
    private var scrolled: Boolean = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<CoordinatorLayout>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsImageRecommendationItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        recommendationSequence = Prefs.getImageRecsItemSequence()
        Prefs.setImageRecsItemSequence(recommendationSequence + 1)

        binding.cardItemErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = View.OnClickListener {
            binding.cardItemProgressBar.visibility = VISIBLE
            binding.cardItemErrorView.visibility = GONE
            getNextItem()
        }

        val transparency = 0xd8000000

        binding.publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.publishOverlayContainer.visibility = GONE
        binding.publishBackgroundView.alpha = if (WikipediaApp.getInstance().currentTheme.isDark) 0.3f else 0.1f

        binding.imageCard.elevation = 0f
        binding.imageCard.strokeColor = ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_border_color)
        binding.imageCard.strokeWidth = DimenUtil.roundedDpToPx(0.5f)

        binding.acceptButton.setOnClickListener {
            buttonClickedMillis = SystemClock.uptimeMillis()
            doPublish(ImageRecommendationsFunnel.RESPONSE_ACCEPT, emptyList())
        }

        binding.rejectButton.setOnClickListener {
            buttonClickedMillis = SystemClock.uptimeMillis()
            ImageRecsDialog.newInstance(ImageRecommendationsFunnel.RESPONSE_REJECT)
                    .show(childFragmentManager, null)
        }

        binding.notSureButton.setOnClickListener {
            buttonClickedMillis = SystemClock.uptimeMillis()
            ImageRecsDialog.newInstance(ImageRecommendationsFunnel.RESPONSE_NOT_SURE)
                    .show(childFragmentManager, null)
        }

        binding.imageCard.setOnClickListener {
            if (recommendation != null) {
                startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + recommendation!!.image, WikiSite(Service.COMMONS_URL)), false, getSuggestionReason()))
                detailsClicked = true
            }
        }

        binding.articleContentContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            scrolled = true
        })

        binding.readMoreButton.setOnClickListener {
            val title = PageTitle(recommendation!!.pageTitle, WikipediaApp.getInstance().wikiSite)
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
        }

        ImageZoomHelper.setViewZoomable(binding.imageView)
        binding.dailyProgressView.setMaximum(DAILY_COUNT_TARGET)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetCoordinatorLayout).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        getNextItem()
        updateContents(null)
    }

    override fun onResume() {
        super.onResume()
        startMillis = SystemClock.uptimeMillis()
    }

    override fun onStart() {
        super.onStart()
        callback().updateActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun maybeShowTooltipSequence() {
        binding.root.post {
            if (!isResumed || !isAdded) {
                return@post
            }
            if (Prefs.shouldShowImageRecsOnboarding()) {
                Prefs.setShowImageRecsOnboarding(false)

                // binding.imageSuggestionContainer.visibility = GONE
                // ViewAnimations.ensureTranslationY(binding.imageSuggestionContainer, binding.imageSuggestionContainer.height)

                val balloon = FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendations_tooltip1), autoDismiss = true, showDismissButton = true)
                balloon.showAlignBottom(binding.articleTitlePlaceholder)
                balloon.relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendations_tooltip2), autoDismiss = true, showDismissButton = true), binding.instructionText)
                        .relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendations_tooltip3), autoDismiss = true, showDismissButton = true), binding.acceptButton)
            } else {
                // binding.imageSuggestionContainer.visibility = VISIBLE
                // ViewAnimations.ensureTranslationY(binding.imageSuggestionContainer, 0)
            }
        }
    }

    private fun getNextItem() {
        if (recommendation != null) {
            return
        }
        disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingImage(WikipediaApp.getInstance().appOrSystemLanguageCode, recommendationSequence)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    recommendation = it
                    ServiceFactory.get(WikipediaApp.getInstance().wikiSite).getInfoByPageId(recommendation!!.pageId.toString())
                }
                .flatMap {
                    recommendation!!.pageTitle = it.query()!!.firstPage()!!.displayTitle(WikipediaApp.getInstance().appOrSystemLanguageCode)
                    ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getSummary(null, recommendation!!.pageTitle).subscribeOn(Schedulers.io())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .retry(10)
                .subscribe({ summary ->
                    updateContents(summary)
                }, { this.setErrorState(it) })!!)
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
                    ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo("File:" + recommendation!!.image, WikipediaApp.getInstance().appOrSystemLanguageCode)
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
                                if (WikipediaApp.getInstance().currentTheme.isDark) {
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
        binding.publishBoltView.visibility = GONE
        binding.publishProgressBarComplete.visibility = GONE
        binding.publishProgressBar.visibility = VISIBLE

        funnel.logSubmit(WikipediaApp.getInstance().language().appLanguageCodes.joinToString(","),
                recommendation!!.pageTitle, recommendation!!.image, getFunnelReason(), response, reasons, detailsClicked, infoClicked, scrolled,
                buttonClickedMillis - startMillis, SystemClock.uptimeMillis() - startMillis,
                if (Prefs.isImageRecsConsentEnabled() && AccountUtil.isLoggedIn) AccountUtil.userName else null,
                Prefs.isImageRecsTeacherMode())
        ImageRecommendationsEvent.logImageRecommendationInteraction(WikipediaApp.getInstance().language().appLanguageCodes.joinToString(","),
                recommendation!!.pageTitle, recommendation!!.image, getFunnelReason(), response, reasons, detailsClicked, infoClicked, scrolled,
                buttonClickedMillis - startMillis, SystemClock.uptimeMillis() - startMillis,
                if (Prefs.isImageRecsConsentEnabled() && AccountUtil.isLoggedIn) AccountUtil.userName else null,
                Prefs.isImageRecsTeacherMode())
        publishSuccess = true
        onSuccess()
    }

    private fun onSuccess() {

        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        var oldCount = Prefs.getImageRecsDailyCount()
        if (day != Prefs.getImageRecsDayId()) {
            // it's a brand new day!
            Prefs.setImageRecsDayId(day)
            oldCount = 0
        }
        val newCount = oldCount + 1
        Prefs.setImageRecsDailyCount(newCount)
        Prefs.setImageRecsItemSequenceSuccess(recommendationSequence + 1)

        val durationBoost = when (newCount) {
            DAILY_COUNT_TARGET -> 5
            else -> 3
        }

        val progressText = when {
            newCount < DAILY_COUNT_TARGET -> getString(R.string.suggested_edits_image_recommendations_task_goal_progress)
            newCount == DAILY_COUNT_TARGET -> getString(R.string.suggested_edits_image_recommendations_task_goal_complete)
            else -> getString(R.string.suggested_edits_image_recommendations_task_goal_surpassed)
        }
        binding.dailyProgressView.update(oldCount, oldCount, DAILY_COUNT_TARGET, getString(R.string.image_recommendations_task_processing))
        showConfetti(newCount == DAILY_COUNT_TARGET)
        val duration = 1000L
        binding.publishProgressBar.alpha = 1f
        binding.publishProgressBar.animate()
                .alpha(0f)
                .duration = duration / 2

        binding.publishProgressBarComplete.alpha = 0f
        binding.publishProgressBarComplete.visibility = VISIBLE
        binding.publishProgressBarComplete.animate()
                .alpha(1f)
                .withEndAction {
                    binding.dailyProgressView.update(oldCount, newCount, DAILY_COUNT_TARGET, progressText)
                    if (newCount >= DAILY_COUNT_TARGET) {
                        binding.publishProgressBarComplete.visibility = GONE
                        binding.publishProgressCheck.visibility = GONE
                        binding.publishBoltView.visibility = VISIBLE
                    }
                }
                .duration = duration / 2

        binding.publishProgressCheck.alpha = 0f
        binding.publishProgressCheck.visibility = VISIBLE
        binding.publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        binding.publishProgressBar.postDelayed({
            if (isAdded) {
                showConfetti(false)
                binding.publishOverlayContainer.visibility = GONE
                callback().nextPage(this)
                callback().logSuccess()
            }
        }, duration * durationBoost)
    }

    private fun showConfetti(shouldShowConfetti: Boolean) {
        binding.successConfettiImage.visibility = if (shouldShowConfetti) VISIBLE else GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Change statusBar and actionBar color
            requireActivity().window.statusBarColor = if (shouldShowConfetti) ResourceUtil.getThemedColor(requireContext(),
                    R.attr.image_recs_confetti_background_color) else Color.TRANSPARENT
            (requireActivity() as AppCompatActivity).supportActionBar!!.setBackgroundDrawable(if (shouldShowConfetti)
                ColorDrawable(ResourceUtil.getThemedColor(requireContext(), R.attr.image_recs_confetti_background_color)) else null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Change navigationBar color
            requireActivity().window.navigationBarColor = if (shouldShowConfetti) ResourceUtil.getThemedColor(requireContext(),
                    R.attr.image_recs_confetti_background_color) else Color.TRANSPARENT
        }
        // Update actionbar menu items
        requireActivity().window.decorView.findViewById<TextView?>(R.id.menu_help).apply {
            visibility = if (shouldShowConfetti) GONE else VISIBLE
        }
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(!shouldShowConfetti)
    }

    override fun publishEnabled(): Boolean {
        return true
    }

    override fun publishOutlined(): Boolean {
        return false
    }

    fun onInfoClicked() {
        infoClicked = true
    }

    private fun getSuggestionReason(): String {
        val hasWikidata = recommendation!!.foundOnWikis.contains("wd")
        val langWikis = recommendation!!.foundOnWikis
                .sortedBy { WikipediaApp.getInstance().language().getLanguageCodeIndex(it) }
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
    }

    private fun getFunnelReason(): String {
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
    }

    private fun getCanonicalName(code: String): String? {
        var canonicalName = siteInfoList?.find { it.code() == code }?.localName()
        if (canonicalName.isNullOrEmpty()) {
            canonicalName = WikipediaApp.getInstance().language().getAppLanguageCanonicalName(code)
        }
        return canonicalName
    }

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }

    companion object {
        const val DAILY_COUNT_TARGET = 10
        private val SUPPORTED_LANGUAGES = arrayOf("en", "de", "fr", "pt", "ru", "fa", "tr", "uk", "ar", "vi", "ceb", "he")
        private var siteInfoList: List<SiteMatrix.SiteInfo>? = null

        fun isFeatureEnabled(): Boolean {
            return AccountUtil.isLoggedIn &&
                    SUPPORTED_LANGUAGES.any { it in WikipediaApp.getInstance().language().appLanguageCodes }
        }

        fun newInstance(): SuggestedEditsItemFragment {
            return ImageRecsFragment()
        }
    }
}
