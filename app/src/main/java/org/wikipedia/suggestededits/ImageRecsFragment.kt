package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.ImageRecommendationsFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecommendationItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.restbase.ImageRecommendationResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper
import java.util.*
import java.util.concurrent.TimeUnit

class ImageRecsFragment : SuggestedEditsItemFragment(), ImageRecsDialog.Callback {
    private var _binding: FragmentSuggestedEditsImageRecommendationItemBinding? = null
    private val binding get() = _binding!!

    var publishing: Boolean = false
    private var publishSuccess: Boolean = false
    private var page: ImageRecommendationResponse? = null

    private val funnel = ImageRecommendationsFunnel()
    private var startMillis: Long = 0
    private var buttonClickedMillis: Long = 0
    private var infoClicked: Boolean = false
    private var detailsClicked: Boolean = false
    private var scrolled: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsImageRecommendationItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardItemErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = View.OnClickListener {
            binding.cardItemProgressBar.visibility = VISIBLE
            binding.cardItemErrorView.visibility = GONE
            getNextItem()
        }

        val transparency = 0xd8000000

        binding.publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.publishOverlayContainer.visibility = GONE

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
            if (page != null) {
                startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + page!!.recommendation.image, WikiSite(Service.COMMONS_URL)), false))
                detailsClicked = true
            }
        }

        binding.articleContentContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            scrolled = true
        })

        binding.dailyProgressView.setMaximum(DAILY_COUNT_TARGET)

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

                FeedbackUtil.showTooltip(requireActivity(), binding.articleTitlePlaceholder, "Review this article to understand its topic.", aboveOrBelow = false, autoDismiss = true)
                        .setOnBalloonDismissListener {

                            // binding.imageSuggestionContainer.visibility = VISIBLE
                            // ViewAnimations.ensureTranslationY(binding.imageSuggestionContainer, 0)

                            FeedbackUtil.showTooltip(requireActivity(), binding.instructionText, "Inspect the image and its associated information.", aboveOrBelow = true, autoDismiss = true)
                                    .setOnBalloonDismissListener {
                                        FeedbackUtil.showTooltip(requireActivity(), binding.acceptButton, "Decide if the image will help readers better understand this topic.", aboveOrBelow = true, autoDismiss = true)
                                    }
                        }
            } else {
                // binding.imageSuggestionContainer.visibility = VISIBLE
                // ViewAnimations.ensureTranslationY(binding.imageSuggestionContainer, 0)
            }
        }
    }

    private fun getNextItem() {
        if (page != null) {
            return
        }
        disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingImage(WikipediaApp.getInstance().appOrSystemLanguageCode)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    this.page = it
                    ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getSummary(null, it.title).subscribeOn(Schedulers.io())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .retry(10)
                .subscribe({ summary ->
                    updateContents(summary)
                }, { this.setErrorState(it) })!!)
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        page = null
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.articleContentContainer.visibility = GONE
        binding.imageSuggestionContainer.visibility = GONE
    }

    private fun updateContents(summary: PageSummary?) {
        binding.cardItemErrorView.visibility = GONE
        binding.articleContentContainer.visibility = if (page != null) VISIBLE else GONE
        binding.imageSuggestionContainer.visibility = if (page != null) VISIBLE else GONE
        binding.cardItemProgressBar.visibility = if (page != null) GONE else VISIBLE
        if (page == null || summary == null) {
            return
        }

        ImageZoomHelper.setViewZoomable(binding.imageView)

        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo("File:" + page!!.recommendation.image, WikipediaApp.getInstance().appOrSystemLanguageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .retry(5)
                .subscribe({ response ->
                    val imageInfo = response.query()!!.firstPage()!!.imageInfo()!!

                    binding.imageView.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(imageInfo.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
                    binding.imageCaptionText.text = if (imageInfo.metadata == null) null else StringUtil.removeHTMLTags(imageInfo.metadata!!.imageDescription())

                    binding.articleTitle.text = StringUtil.fromHtml(summary.displayTitle)
                    binding.articleDescription.text = summary.description
                    binding.articleExtract.text = StringUtil.fromHtml(summary.extractHtml).trim()

                    binding.articleScrollSpacer.post {
                        if (isAdded) {
                            binding.articleScrollSpacer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, binding.imageSuggestionContainer.height)
                        }
                    }

                    val arr = imageInfo.commonsUrl.split('/')
                    binding.imageFileNameText.text = StringUtil.removeUnderscores(UriUtil.decodeURL(arr[arr.size - 1]))

                    binding.imageSuggestionReason.text = StringUtil.fromHtml(getString(R.string.image_recommendations_task_suggestion_reason, page!!.recommendation.note))

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
        if (publishing || publishSuccess || page == null) {
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
                page!!.title, page!!.recommendation.image, response, reasons, detailsClicked, infoClicked, scrolled,
                TimeUnit.MILLISECONDS.toSeconds(buttonClickedMillis - startMillis).toInt(),
                TimeUnit.MILLISECONDS.toSeconds(SystemClock.uptimeMillis() - startMillis).toInt(),
                if (Prefs.isImageRecsConsentEnabled()) AccountUtil.userName else null,
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

        val progressText = when {
            newCount < DAILY_COUNT_TARGET -> getString(R.string.image_recommendations_task_goal_progress)
            newCount == DAILY_COUNT_TARGET -> getString(R.string.image_recommendations_task_goal_complete)
            else -> getString(R.string.image_recommendations_task_goal_surpassed)
        }
        binding.dailyProgressView.update(oldCount, oldCount, DAILY_COUNT_TARGET, getString(R.string.image_recommendations_task_processing))

        val duration = 1000L
        binding.publishProgressBar.alpha = 1f
        binding.publishProgressBar.animate()
                .alpha(0f)
                .duration = duration / 2

        binding.publishProgressBarComplete.alpha = 0f
        binding.publishProgressBarComplete.visibility = VISIBLE
        binding.publishProgressBarComplete.animate()
                .alpha(1f)
                .duration = duration / 2

        binding.publishProgressCheck.alpha = 0f
        binding.publishProgressCheck.visibility = VISIBLE
        binding.publishProgressCheck.animate()
                .alpha(1f)
                .withEndAction {
                    binding.dailyProgressView.update(oldCount, newCount, DAILY_COUNT_TARGET, progressText)
                    if (newCount >= DAILY_COUNT_TARGET) {
                        binding.publishProgressBarComplete.visibility = GONE
                        binding.publishProgressCheck.visibility = GONE
                        binding.publishBoltView.visibility = VISIBLE
                    }
                }
                .duration = duration

        binding.publishProgressBar.postDelayed({
            if (isAdded) {
                binding.publishOverlayContainer.visibility = GONE
                callback().nextPage(this)
                callback().logSuccess()
            }
        }, duration * 3)
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

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }

    companion object {
        const val DAILY_COUNT_TARGET = 20

        fun newInstance(): SuggestedEditsItemFragment {
            return ImageRecsFragment()
        }
    }
}
