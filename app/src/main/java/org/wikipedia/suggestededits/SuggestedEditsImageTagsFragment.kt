package org.wikipedia.suggestededits

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_image_tags_item.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.media.MediaHelper
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.*
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper

class SuggestedEditsImageTagsFragment : SuggestedEditsItemFragment(), CompoundButton.OnCheckedChangeListener {
    var publishing: Boolean = false
    var publishSuccess: Boolean = false
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
            cardItemErrorView.visibility = GONE
            getNextItem()
        }

        val transparency = 0xcc000000
        tagsContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.suggestions_background_color) and 0xffffff))
        imageCaption.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.suggestions_background_color) and 0xffffff))

        publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.suggestions_background_color) and 0xffffff))
        publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.getInstance().currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        publishProgressBar.progressTintList = colorStateList
        publishProgressCheck.imageTintList = colorStateList
        publishProgressText.setTextColor(colorStateList)

        tagsLicenseText.text = StringUtil.fromHtml(getString(R.string.suggested_edits_cc0_notice,
                getString(R.string.terms_of_use_url), getString(R.string.cc_0_url)))
        tagsLicenseText.movementMethod = LinkMovementMethodExt.getInstance()

        getNextItem()
        updateContents()
    }

    override fun onStart() {
        super.onStart()
        parent().updateActionButton()
    }

    private fun getNextItem() {
        if (page != null) {
            return
        }
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

        tagsLicenseText.visibility = GONE
        tagsHintText.visibility = VISIBLE
        ImageZoomHelper.setViewZoomable(imageView)

        imageView.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(page!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))

        tagsChipGroup.removeAllViews()
        val maxTags = 10
        for (label in page!!.imageLabels) {
            if (label.state != "unreviewed") {
                continue
            }
            val chip = Chip(requireContext())
            chip.text = label.label
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.chip_text_color))
            chip.typeface = tagsHintText.typeface
            chip.isCheckable = true
            chip.setCheckedIconResource(R.drawable.ic_chip_check_24px)
            chip.setOnCheckedChangeListener(this)
            chip.tag = label

            // add some padding to the Chip, since our container view doesn't support item spacing yet.
            val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val margin = DimenUtil.roundedDpToPx(8f)
            params.setMargins(margin, margin, margin, margin)
            chip.layoutParams = params

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
                            imageCaption.text = StringUtil.fromHtml(page!!.imageInfo()!!.metadata!!.imageDescription()).toString().trim()
                            imageCaption.visibility = VISIBLE
                        } else {
                            imageCaption.visibility = GONE
                        }
                    }
                })

        parent().updateActionButton()
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageTagsFragment()
        }
    }

    override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
        if (tagsLicenseText.visibility != VISIBLE) {
            tagsLicenseText.visibility = VISIBLE
            tagsHintText.visibility = GONE
        }
        val chip = button as Chip
        if (chip.isChecked) {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
            chip.setTextColor(Color.WHITE)
        } else {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.chip_text_color))
        }
        parent().updateActionButton()
    }

    override fun publish() {
        if (publishing || publishSuccess || tagsChipGroup.childCount == 0) {
            return
        }
        var acceptedCount = 0
        for (i in 0 until tagsChipGroup.childCount) {
            val chip = tagsChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                acceptedCount++
            }
        }
        if (acceptedCount == 0) {
            AlertDialog.Builder(requireContext())
                    .setTitle(R.string.suggested_edits_image_tags_select_title)
                    .setMessage(R.string.suggested_edits_image_tags_select_text)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.description_edit_save) { _, _ ->
                        doPublish()
                    }
                    .show()
            return
        } else {
            doPublish()
        }
    }

    private fun doPublish() {
        var acceptedCount = 0
        var rejectedCount = 0
        val batchBuilder = StringBuilder()
        batchBuilder.append("[")
        for (i in 0 until tagsChipGroup.childCount) {
            if (acceptedCount > 0 || rejectedCount > 0) {
                batchBuilder.append(",")
            }
            val chip = tagsChipGroup.getChildAt(i) as Chip
            val label = chip.tag as MwQueryPage.ImageLabel
            batchBuilder.append("{\"label\":\"")
            batchBuilder.append(label.wikidataId)
            batchBuilder.append("\",\"review\":\"")
            batchBuilder.append(if (chip.isChecked) "accept" else "reject")
            batchBuilder.append("\"}")
            if (chip.isChecked) {
                acceptedCount++
            } else {
                rejectedCount++
            }
        }
        batchBuilder.append("]")

        // -- point of no return --

        publishing = true
        publishSuccess = false

        publishProgressText.setText(R.string.suggested_edits_image_tags_publishing)
        publishProgressCheck.visibility = GONE
        publishOverlayContainer.visibility = VISIBLE

        // kick off the circular animation
        val duration = 2000L
        val animator = ObjectAnimator.ofInt(publishProgressBar, "progress", 0, 1000)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        publishProgressBar.postDelayed({
            if (isAdded && !publishing && publishSuccess) {
                onSuccess()
            }
        }, duration)

        csrfClient.request(false, object : CsrfTokenClient.Callback {
            override fun success(token: String) {
                disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).postReviewImageLabels(page!!.title(), token, batchBuilder.toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doAfterTerminate {
                            publishing = false
                        }
                        .subscribe({
                            // TODO: check anything else in the response?
                            publishSuccess = true
                            if (!animator.isRunning) {
                                // if the animator is still running, let it finish and invoke success() on its own
                                onSuccess()
                            }
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
        publishProgressText.setText(R.string.suggested_edits_image_tags_published)

        Prefs.setSuggestedEditsImageTagsNew(false)

        val duration = 500L
        publishProgressCheck.alpha = 0f
        publishProgressCheck.visibility = VISIBLE
        publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        publishProgressBar.postDelayed({
            if (isAdded) {
                publishOverlayContainer.visibility = GONE
                parent().nextPage()
                setPublishedState()
            }
        }, duration * 2)
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        publishOverlayContainer.visibility = GONE
        FeedbackUtil.showError(requireActivity(), caught)
    }

    private fun setPublishedState() {
        for (i in 0 until tagsChipGroup.childCount) {
            val chip = tagsChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.green_highlight_color))
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
                chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.chip_text_color))
            }
        }
    }

    override fun publishEnabled(): Boolean {
        return !publishSuccess
    }

    override fun publishOutlined(): Boolean {
        var atLeastOneChecked = false
        for (i in 0 until tagsChipGroup.childCount) {
            val chip = tagsChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                atLeastOneChecked = true
                break
            }
        }
        return !atLeastOneChecked
    }
}
