package org.wikipedia.suggestededits

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CompoundButton
import com.google.android.material.chip.Chip
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_image_tags_item.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.media.MediaHelper
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper
import java.lang.StringBuilder

class SuggestedEditsImageTagsFragment : SuggestedEditsItemFragment(), CompoundButton.OnCheckedChangeListener {
    var publishing: Boolean = false
    private var csrfClient: CsrfTokenClient = CsrfTokenClient(WikiSite(Service.COMMONS_URL))
    private var page: MwQueryPage? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_image_tags_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(contentContainer, parent().langFromCode)
        imageView.setLegacyVisibilityHandlingEnabled(true)
        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.visibility = VISIBLE
            getNextItem()
        }

        val transparency = 0xcc000000
        tagsContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), android.R.attr.colorBackground) and 0xffffff))
        imageCaption.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), android.R.attr.colorBackground) and 0xffffff))

        publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), android.R.attr.colorBackground) and 0xffffff))
        publishOverlayContainer.visibility = GONE

        getNextItem()
        updateContents()
    }

    private fun getNextItem() {
        disposables.add(MissingDescriptionProvider.getNextImageWithMissingTags(parent().langFromCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ page ->
                    this.page = page
                    updateContents()
                }, { this.setErrorState(it) })!!)
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        cardItemErrorView.setError(t)
        cardItemErrorView.visibility = VISIBLE
        cardItemProgressBar.visibility = GONE
        contentContainer.visibility = GONE
    }

    private fun updateContents() {
        cardItemErrorView.visibility = GONE
        contentContainer.visibility = if (page != null) VISIBLE else GONE
        cardItemProgressBar.visibility = if (page != null) GONE else VISIBLE
        if (page == null) {
            return
        }

        ImageZoomHelper.setViewZoomable(imageView)

        imageView.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(page!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))

        tagsChipGroup.removeAllViews()
        val maxTags = 10
        for (label in page!!.imageLabels) {
            val chip = Chip(requireContext())
            chip.text = label.label
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.chip_text_color))
            chip.typeface = tagsHintText.typeface
            chip.isCheckable = true
            chip.setCheckedIconResource(R.drawable.ic_chip_check_24px)
            chip.setOnCheckedChangeListener(this)
            chip.tag = label

            tagsChipGroup.addView(chip)
            if (tagsChipGroup.childCount >= maxTags) {
                break
            }
        }

        disposables.add(MediaHelper.getImageCaptions(page!!.title())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { captions ->
                    if (captions.containsKey(parent().langFromCode)) {
                        imageCaption.text = captions[parent().langFromCode]
                        imageCaption.visibility = VISIBLE
                    } else {
                        if (page!!.imageInfo() != null && page!!.imageInfo()!!.metadata != null) {
                            imageCaption.text = StringUtil.fromHtml(page!!.imageInfo()!!.metadata!!.imageDescription())
                            imageCaption.visibility = VISIBLE
                        } else {
                            imageCaption.visibility = GONE
                        }
                    }
                })
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageTagsFragment()
        }
    }

    override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
        val chip = button as Chip
        if (chip.isChecked) {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
            chip.setTextColor(Color.WHITE)
        } else {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.chip_text_color))
        }
    }

    override fun publish() {
        if (publishing) {
            return
        }
        publishing = true
        publishOverlayContainer.visibility = VISIBLE

        val batchBuilder = StringBuilder()
        batchBuilder.append("[")
        for (i in 0 until tagsChipGroup.childCount) {
            if (i > 0) {
                batchBuilder.append(",")
            }
            val chip = tagsChipGroup.getChildAt(i) as Chip
            val label = chip.tag as MwQueryPage.ImageLabel
            batchBuilder.append("{\"label\":\"")
            batchBuilder.append(label.wikidataId)
            batchBuilder.append("\",\"review\":\"")
            batchBuilder.append(if (chip.isChecked) "accept" else "reject")
            batchBuilder.append("\"}")
        }
        batchBuilder.append("]")

        csrfClient.request(false, object : CsrfTokenClient.Callback {
            override fun success(token: String) {
                disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).postReviewImageLabels(page!!.title(), token, batchBuilder.toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doAfterTerminate {
                            publishing = false
                            publishOverlayContainer.visibility = GONE
                        }
                        .subscribe({ response ->
                            // TODO: check anything else in the response?
                            onSuccess()
                        }, { caught ->
                            onError(caught)
                        }))
            }

            override fun failure(caught: Throwable) {
                onError(caught)
            }

            override fun twoFactorPrompt() {
                onError(LoginFailedException(resources.getString(R.string.login_2fa_other_workflow_error_msg)))
            }
        })
    }

    private fun onSuccess() {

        // TODO: animation

        parent().nextPage()
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        FeedbackUtil.showError(requireActivity(), caught)
    }
}
