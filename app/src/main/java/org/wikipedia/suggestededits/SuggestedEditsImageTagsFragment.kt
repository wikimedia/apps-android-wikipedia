package org.wikipedia.suggestededits

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.view.children
import com.google.android.material.chip.Chip
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.FragmentSuggestedEditsImageTagsItemBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.media.MediaHelper
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.*
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil
import java.util.*
import kotlin.collections.ArrayList

class SuggestedEditsImageTagsFragment : SuggestedEditsItemFragment(), CompoundButton.OnCheckedChangeListener, OnClickListener, SuggestedEditsImageTagDialog.Callback {

    private var _binding: FragmentSuggestedEditsImageTagsItemBinding? = null
    private val binding get() = _binding!!

    var publishing: Boolean = false
    var publishSuccess: Boolean = false
    private var page: MwQueryPage? = null
    private val tagList: MutableList<MwQueryPage.ImageLabel> = ArrayList()
    private var wasCaptionLongClicked: Boolean = false
    private var lastSearchTerm: String = ""
    var invokeSource: InvokeSource = InvokeSource.SUGGESTED_EDITS
    private var funnel: EditFunnel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsImageTagsItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setConditionalLayoutDirection(binding.contentContainer, callback().getLangCode())
        binding.cardItemErrorView.backClickListener = OnClickListener { requireActivity().finish() }
        binding.cardItemErrorView.retryClickListener = OnClickListener {
            binding.cardItemProgressBar.visibility = VISIBLE
            binding.cardItemErrorView.visibility = GONE
            getNextItem()
        }

        val transparency = 0xcc000000
        binding.tagsContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.imageCaption.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))

        binding.publishOverlayContainer.setBackgroundColor(transparency.toInt() or (ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color) and 0xffffff))
        binding.publishOverlayContainer.visibility = GONE

        val colorStateList = ColorStateList(arrayOf(intArrayOf()),
                intArrayOf(if (WikipediaApp.instance.currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent)))
        binding.publishProgressBar.progressTintList = colorStateList
        binding.publishProgressBarComplete.progressTintList = colorStateList
        binding.publishProgressCheck.imageTintList = colorStateList
        binding.publishProgressText.setTextColor(colorStateList)

        binding.tagsLicenseText.text = StringUtil.fromHtml(getString(R.string.suggested_edits_cc0_notice,
                getString(R.string.terms_of_use_url), getString(R.string.cc_0_url)))
        binding.tagsLicenseText.movementMethod = LinkMovementMethod.getInstance()

        binding.imageView.setOnClickListener {
            if (Prefs.shouldShowImageZoomTooltip()) {
                Prefs.setShouldShowImageZoomTooltip(false)
                FeedbackUtil.showToastOverView(binding.imageView, getString(R.string.suggested_edits_image_zoom_tooltip), Toast.LENGTH_LONG)
            }
        }

        if (callback().getSinglePage() != null) {
            page = callback().getSinglePage()
        }

        binding.imageCaption.setOnLongClickListener {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getNextItem() {
        if (page != null) {
            return
        }
        disposables.add(EditingSuggestionsProvider.getNextImageWithMissingTags()
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
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.contentContainer.visibility = GONE
    }

    private fun updateContents() {
        binding.cardItemErrorView.visibility = GONE
        binding.contentContainer.visibility = if (page != null) VISIBLE else GONE
        binding.cardItemProgressBar.visibility = if (page != null) GONE else VISIBLE
        if (page == null) {
            return
        }

        funnel = EditFunnel(WikipediaApp.instance, PageTitle(page!!.title(), WikiSite(Service.COMMONS_URL)))

        binding.tagsLicenseText.visibility = GONE
        binding.tagsHintText.visibility = VISIBLE
        ImageZoomHelper.setViewZoomable(binding.imageView)

        ViewUtil.loadImage(binding.imageView, ImageUrlUtil.getUrlForPreferredSize(page!!.imageInfo()!!.thumbUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE))

        disposables.add(MediaHelper.getImageCaptions(page!!.title())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { captions ->
                    if (captions.containsKey(callback().getLangCode())) {
                        binding.imageCaption.text = captions[callback().getLangCode()]
                        binding.imageCaption.visibility = VISIBLE
                    } else {
                        if (page!!.imageInfo() != null && page!!.imageInfo()!!.metadata != null) {
                            binding.imageCaption.text = StringUtil.fromHtml(page!!.imageInfo()!!.metadata!!.imageDescription()).toString().trim()
                            binding.imageCaption.visibility = VISIBLE
                        } else {
                            binding.imageCaption.visibility = GONE
                        }
                    }
                })

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    private fun updateTagChips() {
        val typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        binding.tagsChipGroup.removeAllViews()

        if (!publishSuccess) {
            // add an artificial chip for adding a custom tag
            addChip(null, typeface)
        }

        for (label in tagList) {
            val chip = addChip(label, typeface)
            chip.isChecked = label.isSelected
            if (publishSuccess) {
                chip.isEnabled = false
                if (chip.isChecked) {
                    chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_57))
                    chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.color_group_58))
                } else {
                    chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
                    chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.chip_background_color))
                }
            }
        }

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    private fun addChip(label: MwQueryPage.ImageLabel?, typeface: Typeface): Chip {
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
        chip.setEnsureMinTouchTargetSize(true)
        chip.ensureAccessibleTouchTarget(DimenUtil.dpToPx(48f).toInt())
        chip.tag = label
        if (label != null) {
            chip.isChecked = label.isSelected
        }

        // add some padding to the Chip, since our container view doesn't support item spacing yet.
        val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = DimenUtil.roundedDpToPx(8f)
        params.setMargins(margin, 0, margin, 0)
        chip.layoutParams = params

        binding.tagsChipGroup.addView(chip)
        return chip
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

    override fun onSearchSelect(item: MwQueryPage.ImageLabel) {
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

    override fun onSearchDismiss(searchTerm: String) {
        lastSearchTerm = searchTerm
    }

    override fun publish() {
        if (publishing || publishSuccess || binding.tagsChipGroup.childCount == 0) {
            return
        }

        val acceptedLabels = ArrayList<MwQueryPage.ImageLabel>()
        val iterator = tagList.iterator()
        while (iterator.hasNext()) {
            val tag = iterator.next()
            if (tag.isSelected) {
                acceptedLabels.add(tag)
            } else {
                iterator.remove()
            }
        }
        if (acceptedLabels.isEmpty()) {
            return
        }

        // -- point of no return --

        publishing = true
        publishSuccess = false

        funnel?.logSaveAttempt()

        binding.publishProgressText.setText(R.string.suggested_edits_image_tags_publishing)
        binding.publishProgressCheck.visibility = GONE
        binding.publishOverlayContainer.visibility = VISIBLE
        binding.publishProgressBarComplete.visibility = GONE
        binding.publishProgressBar.visibility = VISIBLE

        val commonsSite = WikiSite(Service.COMMONS_URL)

        disposables.add(CsrfTokenClient(WikiSite(Service.COMMONS_URL)).token
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ token ->
                    val mId = "M" + page!!.pageId()
                    var claimStr = "{\"claims\":["
                    var commentStr = "/* add-depicts: "
                    var first = true
                    for (label in acceptedLabels) {
                        if (!first) {
                            claimStr += ","
                        }
                        if (!first) {
                            commentStr += ","
                        }
                        first = false
                        claimStr += "{\"mainsnak\":" +
                                "{\"snaktype\":\"value\",\"property\":\"P180\"," +
                                "\"datavalue\":{\"value\":" +
                                "{\"entity-type\":\"item\",\"id\":\"${label.wikidataId}\"}," +
                                "\"type\":\"wikibase-entityid\"},\"datatype\":\"wikibase-item\"}," +
                                "\"type\":\"statement\"," +
                                "\"id\":\"${mId}\$${UUID.randomUUID()}\"," +
                                "\"rank\":\"normal\"}"
                        commentStr += label.wikidataId + "|" + label.label.replace("|", "").replace(",", "")
                    }
                    claimStr += "]}"
                    commentStr += " */"

                    disposables.add(ServiceFactory.get(commonsSite).postEditEntity(mId, token, claimStr, commentStr, null)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doAfterTerminate {
                                publishing = false
                            }
                            .subscribe({
                                if (it.entity != null) {
                                    funnel?.logSaved(it.entity!!.lastRevId, invokeSource.name)
                                }
                                publishSuccess = true
                                onSuccess()
                            }, { caught ->
                                onError(caught)
                            })
                    )
                }, {
                    onError(it)
                }))
    }

    private fun onSuccess() {
        SuggestedEditsFunnel.get()!!.success(ADD_IMAGE_TAGS)

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
                    binding.publishProgressText.setText(R.string.suggested_edits_image_tags_published)
                    playSuccessVibration()
                }
                .duration = duration / 2

        binding.publishProgressCheck.alpha = 0f
        binding.publishProgressCheck.visibility = VISIBLE
        binding.publishProgressCheck.animate()
                .alpha(1f)
                .duration = duration

        binding.publishProgressBar.postDelayed({
            if (isAdded) {
                updateLicenseTextShown()
                binding.publishOverlayContainer.visibility = GONE
                callback().nextPage(this)
                callback().logSuccess()
                updateTagChips()
            }
        }, duration * 3)
    }

    private fun onError(caught: Throwable) {
        // TODO: expand this a bit.
        SuggestedEditsFunnel.get()!!.failure(ADD_IMAGE_TAGS)
        funnel?.logError(caught.localizedMessage)
        binding.publishOverlayContainer.visibility = GONE
        FeedbackUtil.showError(requireActivity(), caught)
    }

    private fun playSuccessVibration() {
        binding.imageView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun updateLicenseTextShown() {
        when {
            publishSuccess -> {
                binding.tagsLicenseText.visibility = GONE
                binding.tagsHintText.setText(R.string.suggested_edits_image_tags_published_list)
                binding.tagsHintText.visibility = VISIBLE
            }
            atLeastOneTagChecked() -> {
                binding.tagsLicenseText.visibility = VISIBLE
                binding.tagsHintText.visibility = GONE
            }
            else -> {
                binding.tagsLicenseText.visibility = GONE
                binding.tagsHintText.visibility = GONE
            }
        }
    }

    private fun atLeastOneTagChecked(): Boolean {
        return binding.tagsChipGroup.children.filterIsInstance<Chip>().any { it.isChecked }
    }

    override fun publishEnabled(): Boolean {
        return !publishSuccess && atLeastOneTagChecked()
    }

    override fun publishOutlined(): Boolean {
        if (_binding == null) {
            return false
        }
        return !atLeastOneTagChecked()
    }

    private fun callback(): Callback {
        return FragmentUtil.getCallback(this, Callback::class.java)!!
    }
}
