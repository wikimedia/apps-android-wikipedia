package org.wikipedia.suggestededits

import android.content.pm.ActivityInfo
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
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
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecsItemBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.edit.EditHandler
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.FaceAndColorDetectImageView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewAnimations

class SuggestedEditsImageRecsFragment : SuggestedEditsItemFragment(), MenuProvider, SuggestedEditsImageRecsDialog.Callback {
    private var _binding: FragmentSuggestedEditsImageRecsItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuggestedEditsImageRecsFragmentViewModel by viewModels { SuggestedEditsImageRecsFragmentViewModel.Factory(
        bundleOf(ARG_LANG to WikipediaApp.instance.appOrSystemLanguageCode)) }

    private var infoClicked = false
    private var scrolled = false
    private var resumedMillis = 0L

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<CoordinatorLayout>

    private val requestEdit = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            val revId = result.data?.getLongExtra(EditSectionActivity.EXTRA_REV_ID, 0) ?: 0
            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.edit_saved_successfully))
                .setAction(R.string.edit_published_view) {
                    startActivity(ArticleEditDetailsActivity.newIntent(requireContext(), viewModel.pageTitle, revId))
                }
                .setAnchorView(binding.acceptButton)
                .show()

            viewModel.acceptRecommendation(null, revId)
            callback().nextPage(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsImageRecsItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.cardItemErrorView.backClickListener = OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = OnClickListener {
            viewModel.fetchRecommendation()
        }
        binding.cardItemErrorView.nextClickListener = OnClickListener { callback().nextPage(this) }

        binding.imageCard.elevation = 0f
        binding.imageCard.strokeColor = ResourceUtil.getThemedColor(requireContext(), R.attr.border_color)
        binding.imageCard.strokeWidth = DimenUtil.roundedDpToPx(0.5f)

        binding.imageRecommendationsDepletedText.text = StringUtil.fromHtml(getString(R.string.image_recommendation_depleted))
        binding.imageRecommendationsDepletedText.movementMethod = LinkMovementMethodExt(
            LinkMovementMethodExt.UrlHandler { requireActivity().finish() })

        binding.acceptButton.setOnClickListener {
            doPublish()
        }

        binding.rejectButton.setOnClickListener {
            SuggestedEditsImageRecsDialog.newInstance(0).show(childFragmentManager, null)
        }

        binding.notSureButton.setOnClickListener {
            publish()
        }

        binding.imageCard.setOnClickListener {
            startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + viewModel.recommendation.images[0].image, WikiSite.forLanguageCode(viewModel.langCode)), false))
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
                        is SuggestedEditsImageRecsFragmentViewModel.UiState.Depleted -> onDepletedState()
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

    override fun onResume() {
        super.onResume()
        if (resumedMillis == 0L) {
            resumedMillis = System.currentTimeMillis()
        }
    }

    private fun onLoading() {
        binding.cardItemProgressBar.isVisible = true
        binding.cardItemErrorView.isVisible = false
        binding.bottomSheetCoordinatorLayout.isVisible = false
        binding.articleContentContainer.isVisible = false
        binding.imageRecommendationsDepletedContainer.isVisible = false
    }

    private fun onError(throwable: Throwable) {
        L.e(throwable)
        binding.cardItemProgressBar.isVisible = false
        binding.bottomSheetCoordinatorLayout.isVisible = false
        binding.articleContentContainer.isVisible = false
        binding.imageRecommendationsDepletedContainer.isVisible = false
        binding.cardItemErrorView.isVisible = true
        binding.cardItemErrorView.setError(throwable)
    }

    private fun onLoadSuccess() {
        binding.cardItemProgressBar.isVisible = false
        binding.cardItemErrorView.isVisible = false
        binding.bottomSheetCoordinatorLayout.isVisible = true
        binding.articleContentContainer.isVisible = true

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

        if (!Prefs.suggestedEditsImageRecsOnboardingShown) {
            showTooltipSequence()
        }

        callback().updateActionButton()
    }

    private fun onDepletedState() {
        binding.bottomSheetCoordinatorLayout.isVisible = false
        binding.articleContentContainer.isVisible = false
        binding.cardItemProgressBar.isVisible = false
        binding.cardItemErrorView.isVisible = false
        binding.imageRecommendationsDepletedContainer.isVisible = true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_image_recommendations, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_tutorial -> {
                showTooltipSequence()
                true
            }
            R.id.menu_learn_more -> {
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                true
            }
            R.id.menu_report_feature -> {
                FeedbackUtil.composeFeedbackEmail(requireContext(), getString(R.string.suggested_edits_report_feature_subject))
                true
            }
            else -> false
        }
    }

    private fun showTooltipSequence() {
        binding.root.post {
            if (!isResumed || !isAdded) {
                return@post
            }

            val balloon1 = FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendation_tooltip_1), autoDismiss = true,
                showDismissButton = true, dismissButtonText = R.string.image_recommendation_tooltip_next, countNum = 1, countTotal = 3).apply {
                setOnBalloonInitializedListener {
                    // TODO: send event
                }
            }

            val balloon2 = FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendation_tooltip_2), autoDismiss = true,
                showDismissButton = true, dismissButtonText = R.string.image_recommendation_tooltip_next, countNum = 2, countTotal = 3).apply {
                setOnBalloonInitializedListener {
                    // TODO: send event
                }
            }

            val balloon3 = FeedbackUtil.getTooltip(requireContext(), getString(R.string.image_recommendation_tooltip_3), autoDismiss = true,
                showDismissButton = true, countNum = 3, countTotal = 3).apply {
                setOnBalloonInitializedListener {
                    // TODO: send event
                }
                setOnBalloonDismissListener {
                    Prefs.suggestedEditsImageRecsOnboardingShown = true
                }
            }

            balloon1.showAlignBottom(if (binding.articleDescription.isVisible) binding.articleDescription else binding.articleTitle)
            balloon1.relayShowAlignTop(balloon2, binding.instructionText, 0, DimenUtil.roundedDpToPx(12f)).relayShowAlignBottom(balloon3, binding.acceptButton)
        }
    }

    override fun publish() {
        // the "Publish" button in our case is actually the "skip" button.
        callback().nextPage(this)
    }

    private fun doPublish() {
        if (System.currentTimeMillis() - resumedMillis < MIN_TIME_WARNING_MILLIS) {
            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.image_recommendation_tooltip_warning))
                .setAnchorView(binding.acceptButton)
                .show()
            return
        }

        requestEdit.launch(EditSectionActivity.newIntent(requireContext(), 0, null,
            viewModel.pageTitle, Constants.InvokeSource.EDIT_ADD_IMAGE, null, viewModel.recommendedImageTitle))
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
        viewModel.rejectRecommendation(null, selectedItems)
        publish()
    }

    companion object {
        const val ARG_LANG = "lang"
        const val MIN_TIME_WARNING_MILLIS = 5000
        const val IMAGE_REC_EDIT_COMMENT_TOP = "#suggestededit-image-add-top"
        const val IMAGE_REC_EDIT_COMMENT_INFOBOX = "#suggestededit-image-add-infobox"

        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageRecsFragment()
        }
    }
}
