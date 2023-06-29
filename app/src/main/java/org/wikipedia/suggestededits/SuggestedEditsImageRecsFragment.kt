package org.wikipedia.suggestededits

import android.content.pm.ActivityInfo
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecsItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.FaceAndColorDetectImageView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewAnimations

class SuggestedEditsImageRecsFragment : SuggestedEditsItemFragment(), SuggestedEditsImageRecsDialog.Callback {
    private var _binding: FragmentSuggestedEditsImageRecsItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuggestedEditsImageRecsFragmentViewModel by viewModels { SuggestedEditsImageRecsFragmentViewModel.Factory(
        bundleOf(ARG_LANG to WikipediaApp.instance.appOrSystemLanguageCode)) }

    private var publishing = false
    private var publishSuccess = false

    private var infoClicked = false
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
            viewModel.fetchRecommendation()
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
            SuggestedEditsImageRecsDialog.newInstance(0).show(childFragmentManager, null)
        }

        binding.notSureButton.setOnClickListener {
            publish()
        }

        binding.imageCard.setOnClickListener {
            startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + viewModel.recommendation.images[0].displayFilename, WikiSite.forLanguageCode(viewModel.langCode)), false))
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
            val title = PageTitle(viewModel.recommendation.titleText, WikiSite.forLanguageCode(viewModel.langCode))
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), HistoryEntry(title, HistoryEntry.SOURCE_SUGGESTED_EDITS), title))
        }

        ImageZoomHelper.setViewZoomable(binding.imageView)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetCoordinatorLayout).apply {
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        viewLifecycleOwner.lifecycleScope.launch {
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
        binding.cardItemProgressBar.isVisible = true
        binding.cardItemErrorView.isVisible = false
        binding.bottomSheetCoordinatorLayout.isVisible = false
        binding.articleContentContainer.isVisible = false
        binding.publishOverlayContainer.isVisible = false
    }

    private fun onError(throwable: Throwable) {
        L.e(throwable)
        binding.cardItemProgressBar.isVisible = false
        binding.bottomSheetCoordinatorLayout.isVisible = false
        binding.articleContentContainer.isVisible = false
        binding.publishOverlayContainer.isVisible = false
        binding.cardItemErrorView.isVisible = true
        binding.cardItemErrorView.setError(throwable)
    }

    private fun onLoadSuccess() {
        binding.cardItemProgressBar.isVisible = false
        binding.cardItemErrorView.isVisible = false
        binding.bottomSheetCoordinatorLayout.isVisible = true
        binding.articleContentContainer.isVisible = true
        binding.publishOverlayContainer.isVisible = false

        binding.articleTitle.text = StringUtil.fromHtml(viewModel.summary.displayTitle)
        binding.articleDescription.text = viewModel.summary.description
        binding.articleExtract.text = StringUtil.fromHtml(viewModel.summary.extractHtml).trim()

        var thumbUrl = ImageUrlUtil.getUrlForPreferredSize(viewModel.recommendation.images[0].metadata!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)
        if (thumbUrl.startsWith("//")) {
            thumbUrl = "https:$thumbUrl"
        }

        binding.imageView.loadImage(Uri.parse(thumbUrl),
            roundedCorners = false, cropped = false, listener = object : FaceAndColorDetectImageView.OnImageLoadListener {
                override fun onImageLoaded(palette: Palette, bmpWidth: Int, bmpHeight: Int) {
                    if (isAdded) {
                        var color1 = palette.getLightVibrantColor(ContextCompat.getColor(requireContext(), R.color.gray600))
                        var color2 = palette.getLightMutedColor(ContextCompat.getColor(requireContext(), R.color.gray300))
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

        binding.imageCaptionText.text = viewModel.recommendation.images.first().metadata?.caption.orEmpty().trim()
        binding.imageCaptionText.isVisible = binding.imageCaptionText.text.isNotEmpty()

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

        binding.imageFileNameText.text = viewModel.recommendation.images.first().displayFilename

        binding.imageSuggestionReason.text = viewModel.recommendation.images.first().metadata?.reason

        ViewAnimations.fadeIn(binding.imageSuggestionContainer)

        maybeShowTooltipSequence()

        callback().updateActionButton()
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

    override fun publish() {
        // the "Publish" button in our case is actually the "skip" button.
        callback().nextPage(this)
    }

    private fun doPublish(response: Int, reasons: List<Int>) {
        if (publishing || publishSuccess) {
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

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }

    fun onInfoClicked() {
        infoClicked = true
    }

    override fun onDialogSubmit(response: Int, selectedItems: List<Int>) {
        // TODO
        publish()
    }

    companion object {
        const val ARG_LANG = "lang"

        fun isFeatureEnabled(): Boolean {
            return AccountUtil.isLoggedIn &&
                    ReleaseUtil.isPreBetaRelease
        }

        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageRecsFragment()
        }
    }
}
