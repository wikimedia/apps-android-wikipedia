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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_image_recommendation_item.*
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.ImageRecommendationResponse
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper

class SuggestedEditsImageRecommendationFragment : SuggestedEditsItemFragment(), SuggestedEditsImageRecommendationDialog.Callback {

    var publishing: Boolean = false
    var publishSuccess: Boolean = false
    private var csrfClient: CsrfTokenClient = CsrfTokenClient(WikiSite(Service.COMMONS_URL))
    private var page: ImageRecommendationResponse? = null

    var invokeSource: InvokeSource = InvokeSource.SUGGESTED_EDITS
    private var funnel: EditFunnel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_image_recommendation_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardItemErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        cardItemErrorView.retryClickListener = View.OnClickListener {
            cardItemProgressBar.visibility = VISIBLE
            cardItemErrorView.visibility = GONE
            getNextItem()
        }

        val transparency = 0xcc000000

        publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.getInstance().currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        publishProgressBar.progressTintList = colorStateList
        publishProgressBarComplete.progressTintList = colorStateList
        publishProgressCheck.imageTintList = colorStateList
        publishProgressText.setTextColor(colorStateList)

        imageCard.elevation = 0f
        imageCard.strokeColor = ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_de_emphasised_color)
        imageCard.strokeWidth = DimenUtil.roundedDpToPx(0.5f)

        acceptButton.setOnClickListener {
            doPublish(true)
        }

        rejectButton.setOnClickListener {
            SuggestedEditsImageRecommendationDialog().show(childFragmentManager, null)
        }

        notSureButton.setOnClickListener {
            SuggestedEditsImageRecommendationDialog().show(childFragmentManager, null)
        }

        imageClickTarget.setOnClickListener {
            if (page != null) {
                startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle("File:" + page!!.imageTitle, WikiSite(Service.COMMONS_URL))))
            }
        }

        getNextItem()
        updateContents()
    }

    override fun onStart() {
        super.onStart()
        callback().updateActionButton()
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
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = VISIBLE
        cardItemProgressBar.visibility = GONE
        articleContentContainer.visibility = GONE
    }

    private fun updateContents() {
        cardItemErrorView.visibility = GONE
        articleContentContainer.visibility = if (page != null) VISIBLE else GONE
        cardItemProgressBar.visibility = if (page != null) GONE else VISIBLE
        if (page == null) {
            return
        }

        funnel = EditFunnel(WikipediaApp.getInstance(), PageTitle(page!!.title, WikiSite(Service.COMMONS_URL)))

        ImageZoomHelper.setViewZoomable(imageView)

        disposables.add(Observable.zip(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo("File:" + page!!.imageTitle, WikipediaApp.getInstance().appOrSystemLanguageCode).subscribeOn(Schedulers.io()),
                        ServiceFactory.getRest(WikipediaApp.getInstance().wikiSite).getSummary(null, page!!.title).subscribeOn(Schedulers.io()),
                        { imageInfoResponse, summaryResponse -> Pair(imageInfoResponse, summaryResponse)
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe { pair ->

                    val imageInfo = pair.first.query()!!.firstPage()!!.imageInfo()!!
                    val summary = pair.second

                    imageView.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(imageInfo.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
                    imageCaptionText.text = if (imageInfo.metadata == null) null else StringUtil.removeHTMLTags(imageInfo.metadata!!.imageDescription())

                    articleTitle.text = StringUtil.fromHtml(summary.displayTitle)
                    articleDescription.text = summary.description
                    articleExtract.text = StringUtil.fromHtml(summary.extractHtml).trim()

                    articleScrollSpacer.post {
                        if (isAdded) {
                            articleScrollSpacer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, imageSuggestionContainer.height)
                        }
                    }

                    val arr = imageInfo.commonsUrl.split('/')
                    imageFileNameText.text = UriUtil.decodeURL(arr[arr.size - 1])
                })

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

    override fun onDialogSubmit() {
        doPublish(true)
    }

    fun doPublish(accept: Boolean) {
        if (publishing || publishSuccess) {
            return
        }

        // -- point of no return --

        publishing = true
        publishSuccess = false

        funnel?.logSaveAttempt()

        // publishProgressText.setText(R.string.suggested_edits_image_tags_publishing)
        publishProgressCheck.visibility = GONE
        publishOverlayContainer.visibility = VISIBLE
        publishProgressBarComplete.visibility = GONE
        publishProgressBar.visibility = VISIBLE

        // funnel?.logSaved(...)
        publishSuccess = true
        onSuccess()
        // or onError(caught)

        /*
        csrfClient.request(false, object : CsrfTokenClient.Callback {
            override fun success(token: String) {

                // funnel?.logSaved(...)
                publishSuccess = true
                onSuccess()
                // or onError(caught)

            }

            override fun failure(caught: Throwable) {
                onError(caught)
            }

            override fun twoFactorPrompt() {
                onError(LoginFailedException(resources.getString(R.string.login_2fa_other_workflow_error_msg)))
            }
        })
        */
    }

    private fun onSuccess() {
        SuggestedEditsFunnel.get().success(DescriptionEditActivity.Action.IMAGE_RECOMMENDATION)

        val duration = 500L
        publishProgressBar.alpha = 1f
        publishProgressBar.animate()
                .alpha(0f)
                .duration = duration / 2

        publishProgressBarComplete.alpha = 0f
        publishProgressBarComplete.visibility = VISIBLE
        publishProgressBarComplete.animate()
                .alpha(1f)
                .withEndAction {
                    // publishProgressText.setText(R.string.suggested_edits_image_tags_published)
                }
                .duration = duration / 2

        publishProgressCheck.alpha = 0f
        publishProgressCheck.visibility = VISIBLE
        publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        publishProgressBar.postDelayed({
            if (isAdded) {
                publishOverlayContainer.visibility = GONE
                callback().nextPage(this)
                callback().logSuccess()
            }
        }, duration * 3)
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        SuggestedEditsFunnel.get().failure(DescriptionEditActivity.Action.IMAGE_RECOMMENDATION)
        funnel?.logError(caught.localizedMessage)
        publishOverlayContainer.visibility = GONE
        FeedbackUtil.showError(requireActivity(), caught)
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
