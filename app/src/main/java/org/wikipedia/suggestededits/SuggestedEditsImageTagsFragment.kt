package org.wikipedia.suggestededits

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_image_tags_item.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.media.MediaHelper
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.MissingDescriptionProvider
import org.wikipedia.util.*
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil
import java.util.*
import kotlin.collections.ArrayList

class SuggestedEditsImageTagsFragment : SuggestedEditsItemFragment(), CompoundButton.OnCheckedChangeListener, OnClickListener, SuggestedEditsImageTagDialog.Callback {
    interface Callback {
        fun getLangCode(): String
        fun getSinglePage(): MwQueryPage?
        fun updateActionButton()
        fun nextPage(sourceFragment: Fragment?)
    }

    var publishing: Boolean = false
    var publishSuccess: Boolean = false
    private var csrfClient: CsrfTokenClient = CsrfTokenClient(WikiSite(Service.COMMONS_URL))
    private var page: MwQueryPage? = null
    private val tagList: MutableList<MwQueryPage.ImageLabel> = ArrayList()
    private var wasCaptionLongClicked: Boolean = false
    private var lastSearchTerm: String = ""
    private var funnel: EditFunnel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_image_tags_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(contentContainer, callback().getLangCode())
        cardItemErrorView.setBackClickListener { requireActivity().finish() }
        cardItemErrorView.setRetryClickListener {
            cardItemProgressBar.visibility = VISIBLE
            cardItemErrorView.visibility = GONE
            getNextItem()
        }

        val transparency = 0xcc000000
        tagsContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        imageCaption.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))

        publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.getInstance().currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        publishProgressBar.progressTintList = colorStateList
        publishProgressBarComplete.progressTintList = colorStateList
        publishProgressCheck.imageTintList = colorStateList
        publishProgressText.setTextColor(colorStateList)

        tagsLicenseText.text = StringUtil.fromHtml(getString(R.string.suggested_edits_cc0_notice,
                getString(R.string.terms_of_use_url), getString(R.string.cc_0_url)))
        tagsLicenseText.movementMethod = LinkMovementMethodExt.getInstance()

        imageView.setOnClickListener {
            if (Prefs.shouldShowImageZoomTooltip()) {
                Prefs.setShouldShowImageZoomTooltip(false)
                FeedbackUtil.showToastOverView(imageView, getString(R.string.suggested_edits_image_zoom_tooltip), Toast.LENGTH_LONG)
            }
        }

        if (callback().getSinglePage() != null) {
            page = callback().getSinglePage()
        }

        imageCaption.setOnLongClickListener {
            wasCaptionLongClicked = true
            false
        }

        getNextItem()
        updateContents()
        updateTagChips()
    }

    override fun onStart() {
        super.onStart()
        callback().updateActionButton()
    }

    private fun getNextItem() {
        if (page != null) {
            return
        }
        disposables.add(MissingDescriptionProvider.getNextImageWithMissingTags(callback().getLangCode())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ page ->
                    this.page = page
                    updateContents()
                    updateTagChips()
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

        funnel = EditFunnel(WikipediaApp.getInstance(), PageTitle(page!!.title(), WikiSite(Service.COMMONS_URL)))

        tagsLicenseText.visibility = GONE
        tagsHintText.visibility = VISIBLE
        ImageZoomHelper.setViewZoomable(imageView)

        ViewUtil.loadImage(imageView, ImageUrlUtil.getUrlForPreferredSize(page!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE))

        disposables.add(MediaHelper.getImageCaptions(page!!.title())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { captions ->
                    if (captions.containsKey(callback().getLangCode())) {
                        imageCaption.text = captions[callback().getLangCode()]
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

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    private fun updateTagChips() {
        val typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        tagsChipGroup.removeAllViews()

        // add an artificial chip for adding a custom tag
        addChip(null, typeface)

        for (label in tagList) {
            addChip(label, typeface)
        }

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    private fun addChip(label: MwQueryPage.ImageLabel?, typeface: Typeface) {
        val chip = Chip(requireContext())
        chip.text = label?.label ?: getString(R.string.suggested_edits_image_tags_add_tag)
        chip.textAlignment = TEXT_ALIGNMENT_CENTER
        chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
        chip.chipStrokeWidth = DimenUtil.dpToPx(1f)
        chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
        chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_primary_color))
        chip.typeface = typeface
        chip.isCheckable = true
        chip.setChipIconResource(R.drawable.ic_chip_add_24px)
        chip.iconEndPadding = 0f
        chip.textStartPadding = DimenUtil.dpToPx(2f)
        chip.chipIconSize = DimenUtil.dpToPx(24f)
        chip.chipIconTint = ColorStateList.valueOf(ResourceUtil.getThemedColor(requireContext(), R.attr.material_theme_de_emphasised_color))
        chip.setCheckedIconResource(R.drawable.ic_chip_check_24px)
        chip.setOnCheckedChangeListener(this)
        chip.setOnClickListener(this)
        chip.tag = label
        if (label != null) {
            chip.isChecked = label.isSelected
        }

        // add some padding to the Chip, since our container view doesn't support item spacing yet.
        val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = DimenUtil.roundedDpToPx(8f)
        params.setMargins(margin, 0, margin, 0)
        chip.layoutParams = params

        tagsChipGroup.addView(chip)
    }

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageTagsFragment()
        }
    }

    override fun onClick(v: View?) {
        val chip = v as Chip
        if (chip.tag == null) {
            // they clicked the chip to add a new tag, so cancel out the check changing...
            chip.isChecked = !chip.isChecked
            // and launch the selection dialog for the custom tag.
            SuggestedEditsImageTagDialog.newInstance(wasCaptionLongClicked, lastSearchTerm).show(childFragmentManager, null)
        }
    }

    override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
        val chip = button as Chip
        if (chip.isChecked) {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_55))
            chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_56))
            chip.isChipIconVisible = false
        } else {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            chip.isChipIconVisible = true
        }
        if (chip.tag != null) {
            (chip.tag as MwQueryPage.ImageLabel).isSelected = chip.isChecked
        }

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    override fun onSelect(item: MwQueryPage.ImageLabel, searchTerm: String) {
        lastSearchTerm = searchTerm
        var exists = false
        for (tag in tagList) {
            if (tag.wikidataId == item.wikidataId) {
                exists = true
                tag.isSelected = true
                break
            }
        }
        if (!exists) {
            item.isSelected = true
            tagList.add(item)
        }
        updateTagChips()
    }

    override fun publish() {
        if (publishing || publishSuccess || tagsChipGroup.childCount == 0) {
            return
        }

        val acceptedLabels = ArrayList<MwQueryPage.ImageLabel>()
        for (label in tagList) {
            if (label.isSelected) {
                acceptedLabels.add(label)
            }
        }
        if (acceptedLabels.isEmpty()) {
            return
        }

        // -- point of no return --

        publishing = true
        publishSuccess = false

        funnel?.logSaveAttempt()

        publishProgressText.setText(R.string.suggested_edits_image_tags_publishing)
        publishProgressCheck.visibility = GONE
        publishOverlayContainer.visibility = VISIBLE
        publishProgressBarComplete.visibility = GONE
        publishProgressBar.visibility = VISIBLE

        val commonsSite = WikiSite(Service.COMMONS_URL)

        csrfClient.request(false, object : CsrfTokenClient.Callback {
            override fun success(token: String) {

                val claimObservables = ArrayList<ObservableSource<MwPostResponse>>()
                val claimComments = ArrayList<String>()
                for (label in acceptedLabels) {
                    val claimTemplate = "{\"mainsnak\":" +
                            "{\"snaktype\":\"value\",\"property\":\"P180\"," +
                            "\"datavalue\":{\"value\":" +
                            "{\"entity-type\":\"item\",\"id\":\"${label.wikidataId}\"}," +
                            "\"type\":\"wikibase-entityid\"},\"datatype\":\"wikibase-item\"}," +
                            "\"type\":\"statement\"," +
                            "\"id\":\"M${page!!.pageId()}\$${UUID.randomUUID()}\"," +
                            "\"rank\":\"normal\"}"

                    val comment = if (label.isCustom) SuggestedEditsFunnel.SUGGESTED_EDITS_IMAGE_TAG_CUSTOM_COMMENT else SuggestedEditsFunnel.SUGGESTED_EDITS_IMAGE_TAG_AUTO_COMMENT
                    claimObservables.add(ServiceFactory.get(commonsSite).postSetClaim(claimTemplate, token, comment, null))
                    claimComments.add(comment)
                }

                disposables.add(Observable.zip(claimObservables) { responses ->
                    for (res in responses) {
                        if (res is MwPostResponse) {
                            if (res.pageInfo != null) {
                                funnel?.logSaved(res.pageInfo!!.lastRevId, if (claimComments.isEmpty()) "" else claimComments.removeAt(0))
                            }
                        }
                    }
                    responses[0]
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doAfterTerminate {
                            publishing = false
                        }
                        .subscribe({ _ ->
                            // TODO: check anything else in the response?
                            publishSuccess = true
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
        Prefs.setSuggestedEditsImageTagsNew(false)
        SuggestedEditsFunnel.get().success(ADD_IMAGE_TAGS)

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
                    publishProgressText.setText(R.string.suggested_edits_image_tags_published)
                    playSuccessVibration()
                }
                .duration = duration / 2

        publishProgressCheck.alpha = 0f
        publishProgressCheck.visibility = VISIBLE
        publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        publishProgressBar.postDelayed({
            if (isAdded) {
                
                for (i in 0 until tagsChipGroup.childCount) {
                    val chip = tagsChipGroup.getChildAt(i) as Chip
                    chip.isEnabled = false
                }
                updateLicenseTextShown()

                publishOverlayContainer.visibility = GONE
                callback().nextPage(this)
                setPublishedState()
            }
        }, duration * 3)
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        SuggestedEditsFunnel.get().failure(ADD_IMAGE_TAGS)
        funnel?.logError(caught.localizedMessage)
        publishOverlayContainer.visibility = GONE
        FeedbackUtil.showError(requireActivity(), caught)
    }

    private fun setPublishedState() {
        for (i in 0 until tagsChipGroup.childCount) {
            val chip = tagsChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_57))
                chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_58))
            } else {
                chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
                chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
            }
        }
    }

    private fun playSuccessVibration() {
        imageView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun updateLicenseTextShown() {
        when {
            publishSuccess -> {
                tagsLicenseText.visibility = GONE
                tagsHintText.setText(R.string.suggested_edits_image_tags_published_list)
                tagsHintText.visibility = VISIBLE
            }
            atLeastOneTagChecked() -> {
                tagsLicenseText.visibility = VISIBLE
                tagsHintText.visibility = GONE
            }
            else -> {
                tagsLicenseText.visibility = GONE
                tagsHintText.visibility = GONE
            }
        }
    }

    private fun atLeastOneTagChecked(): Boolean {
        var atLeastOneChecked = false
        for (i in 0 until tagsChipGroup.childCount) {
            val chip = tagsChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                atLeastOneChecked = true
                break
            }
        }
        return atLeastOneChecked
    }

    override fun publishEnabled(): Boolean {
        var acceptedCount = 0
        for (i in 0 until tagsChipGroup.childCount) {
            val chip = tagsChipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                acceptedCount++
            }
        }
        return !publishSuccess && acceptedCount > 0
    }

    override fun publishOutlined(): Boolean {
        if (tagsChipGroup == null) {
            return false
        }
        return !atLeastOneTagChecked()
    }

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }
}
