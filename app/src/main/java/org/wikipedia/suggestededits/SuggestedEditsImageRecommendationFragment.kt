package org.wikipedia.suggestededits

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.ImageRecommendationsFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecommendationItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.ImageRecommendationResponse
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper

class SuggestedEditsImageRecommendationFragment : SuggestedEditsItemFragment(), SuggestedEditsImageRecommendationDialog.Callback {
    private var _binding: FragmentSuggestedEditsImageRecommendationItemBinding? = null
    private val binding get() = _binding!!

    var publishing: Boolean = false
    private var publishSuccess: Boolean = false
    private var page: ImageRecommendationResponse? = null

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

        val transparency = 0xcc000000

        binding.publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.getInstance().currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        binding.publishProgressBar.progressTintList = colorStateList
        binding.publishProgressBarComplete.progressTintList = colorStateList
        binding.publishProgressCheck.imageTintList = colorStateList
        binding.publishProgressText.setTextColor(colorStateList)

        binding.imageCard.elevation = 0f
        binding.imageCard.strokeColor = ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_de_emphasised_color)
        binding.imageCard.strokeWidth = DimenUtil.roundedDpToPx(0.5f)

        binding.acceptButton.setOnClickListener {
            doPublish(ImageRecommendationsFunnel.RESPONSE_ACCEPT, emptyList())
        }

        binding.rejectButton.setOnClickListener {
            SuggestedEditsImageRecommendationDialog.newInstance(ImageRecommendationsFunnel.RESPONSE_REJECT)
                    .show(childFragmentManager, null)
        }

        binding.notSureButton.setOnClickListener {
            SuggestedEditsImageRecommendationDialog.newInstance(ImageRecommendationsFunnel.RESPONSE_NOT_SURE)
                    .show(childFragmentManager, null)
        }

        binding.imageClickTarget.setOnClickListener {
            if (page != null) {
                startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + page!!.imageTitle, WikiSite(Service.COMMONS_URL))))
                detailsClicked = true
            }
        }

        binding.articleContentContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            scrolled = true
            L.d(">>>>>>>>>> yes.")
        })

        getNextItem()
        updateContents()
    }

    override fun onStart() {
        super.onStart()
        callback().updateActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getNextItem() {
        if (page != null) {
            return
        }
        disposables.add(EditingSuggestionsProvider.getNextArticleWithMissingImage(WikipediaApp.getInstance().appOrSystemLanguageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    this.page = response
                    updateContents()
                }, { this.setErrorState(it) })!!)
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.articleContentContainer.visibility = GONE
        binding.imageSuggestionContainer.visibility = GONE
    }

    private fun updateContents() {
        binding.cardItemErrorView.visibility = GONE
        binding.articleContentContainer.visibility = if (page != null) VISIBLE else GONE
        binding.imageSuggestionContainer.visibility = if (page != null) VISIBLE else GONE
        binding.cardItemProgressBar.visibility = if (page != null) GONE else VISIBLE
        if (page == null) {
            return
        }

        ImageZoomHelper.setViewZoomable(binding.imageView)

        disposables.add(Observable.zip(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo("File:" + page!!.imageTitle, WikipediaApp.getInstance().appOrSystemLanguageCode).subscribeOn(Schedulers.io()),
                        ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getSummary(null, page!!.title).subscribeOn(Schedulers.io()),
                        { imageInfoResponse, summaryResponse -> Pair(imageInfoResponse, summaryResponse)
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pair ->

                    val imageInfo = pair.first.query()!!.firstPage()!!.imageInfo()!!
                    val summary = pair.second

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
                    binding.imageFileNameText.text = UriUtil.decodeURL(arr[arr.size - 1])
                }, { setErrorState(it) }))

        callback().updateActionButton()
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageRecommendationFragment()
        }
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
        binding.publishProgressBarComplete.visibility = GONE
        binding.publishProgressBar.visibility = VISIBLE

        ImageRecommendationsFunnel().logSubmit(WikipediaApp.getInstance().appOrSystemLanguageCode, page!!.title, page!!.imageTitle,
                response, reasons, detailsClicked, scrolled, /* TODO: AccountUtil.userName */ null, Prefs.isImageRecsTeacherMode())

        publishSuccess = true
        onSuccess()
    }

    private fun onSuccess() {
        SuggestedEditsFunnel.get().success(DescriptionEditActivity.Action.IMAGE_RECOMMENDATION)

        val duration = 500L
        binding.publishProgressBar.alpha = 1f
        binding.publishProgressBar.animate()
                .alpha(0f)
                .duration = duration / 2

        binding.publishProgressBarComplete.alpha = 0f
        binding.publishProgressBarComplete.visibility = VISIBLE
        binding.publishProgressBarComplete.animate()
                .alpha(1f)
                .withEndAction {
                    // publishProgressText.setText(R.string.suggested_edits_image_tags_published)
                }
                .duration = duration / 2

        binding.publishProgressCheck.alpha = 0f
        binding.publishProgressCheck.visibility = VISIBLE
        binding.publishProgressCheck.animate()
                .alpha(1f)
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

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }
}
