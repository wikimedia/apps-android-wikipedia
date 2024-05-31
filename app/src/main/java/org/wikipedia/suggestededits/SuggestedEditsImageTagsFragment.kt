package org.wikipedia.suggestededits

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.FragmentSuggestedEditsImageTagsItemBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditFragment
import org.wikipedia.gallery.ImageInfo
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil
import java.util.UUID

class SuggestedEditsImageTagsFragment : SuggestedEditsItemFragment(), CompoundButton.OnCheckedChangeListener, OnClickListener, SuggestedEditsImageTagDialog.Callback {

    private var _binding: FragmentSuggestedEditsImageTagsItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SuggestedEditsImageTagsViewModel by viewModels()

    var publishing = false
    private var publishSuccess = false
    private var page: MwQueryPage? = null
    private val pageTitle get() = PageTitle(page!!.title, Constants.commonsWikiSite)
    private val tagList = mutableListOf<ImageTag>()
    private var wasCaptionLongClicked = false
    private var lastSearchTerm = ""
    var invokeSource = InvokeSource.SUGGESTED_EDITS

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

        val colorStateList = ColorStateList.valueOf(if (WikipediaApp.instance.currentTheme.isDark) Color.WHITE else ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color))
        binding.publishProgressBar.progressTintList = colorStateList
        binding.publishProgressBarComplete.progressTintList = colorStateList
        ImageViewCompat.setImageTintList(binding.publishProgressCheck, colorStateList)
        binding.publishProgressText.setTextColor(colorStateList)

        binding.tagsLicenseText.text = StringUtil.fromHtml(getString(R.string.suggested_edits_cc0_notice,
                getString(R.string.terms_of_use_url), getString(R.string.cc_0_url)))
        binding.tagsLicenseText.movementMethod = LinkMovementMethodCompat.getInstance()

        binding.imageView.setOnClickListener {
            if (Prefs.showImageZoomTooltip) {
                Prefs.showImageZoomTooltip = false
                FeedbackUtil.showToastOverView(binding.imageView, getString(R.string.suggested_edits_image_zoom_tooltip), Toast.LENGTH_LONG)
            } else {
                val pageTitle = pageTitle
                pageTitle.wikiSite = WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)
                startActivity(FilePageActivity.newIntent(requireContext(), pageTitle))
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
        updateTagChips()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> {
                                page = it.data.first
                                updateContents(it.data.first.imageInfo(), it.data.second)
                                updateTagChips()
                            }
                            is Resource.Error -> setErrorState(it.throwable)
                        }
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

    private fun getNextItem() {
        if (page != null) {
            return
        }
        viewModel.findNextSuggestedEditsItem(callback().getLangCode())
    }

    private fun onLoading() {
        binding.cardItemErrorView.visibility = GONE
        binding.cardItemProgressBar.visibility = VISIBLE
        binding.contentContainer.visibility = GONE
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.cardItemErrorView.setError(t)
        binding.cardItemErrorView.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.contentContainer.visibility = GONE
    }

    private fun updateContents(imageInfo: ImageInfo?, caption: String?) {
        binding.cardItemErrorView.visibility = GONE
        binding.contentContainer.visibility = VISIBLE
        binding.cardItemProgressBar.visibility = GONE
        binding.tagsLicenseText.visibility = GONE
        binding.tagsHintText.visibility = VISIBLE
        ImageZoomHelper.setViewZoomable(binding.imageView)
        ViewUtil.loadImage(binding.imageView, ImageUrlUtil.getUrlForPreferredSize(imageInfo?.thumbUrl.orEmpty(), Constants.PREFERRED_CARD_THUMBNAIL_SIZE))
        if (!caption.isNullOrEmpty()) {
            binding.imageCaption.text = caption
            binding.imageCaption.visibility = VISIBLE
        } else {
            if (imageInfo?.metadata != null) {
                binding.imageCaption.text = StringUtil.fromHtml(imageInfo.metadata.imageDescription()).toString().trim()
                binding.imageCaption.visibility = VISIBLE
                binding.imageView.contentDescription = binding.imageCaption.text
            } else {
                binding.imageCaption.visibility = GONE
            }
        }
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
                    chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.addition_color))
                    chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.success_color))
                } else {
                    chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.border_color))
                    chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.border_color))
                }
            }
        }

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    private fun addChip(label: ImageTag?, typeface: Typeface): Chip {
        val chip = Chip(requireContext())
        chip.text = label?.label ?: getString(R.string.suggested_edits_image_tags_add_tag)
        chip.textAlignment = TEXT_ALIGNMENT_CENTER
        chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.border_color))
        chip.chipStrokeWidth = DimenUtil.dpToPx(1f)
        chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.border_color))
        chip.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_color))
        chip.typeface = typeface
        chip.isCheckable = true
        chip.setChipIconResource(R.drawable.ic_chip_add_24px)
        chip.iconEndPadding = 0f
        chip.textStartPadding = DimenUtil.dpToPx(2f)
        chip.chipIconSize = DimenUtil.dpToPx(24f)
        chip.chipIconTint = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.placeholder_color)
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

    override fun onBackPressed(): Boolean {
        if (!atLeastOneTagChecked()) {
            return true
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setCancelable(false)
            .setTitle(R.string.talk_new_topic_exit_dialog_title)
            .setMessage(R.string.suggested_edits_image_tags_exit_dialog_message)
            .setPositiveButton(R.string.edit_abandon_confirm_yes) { _, _ -> requireActivity().finish() }
            .setNegativeButton(R.string.edit_abandon_confirm_no, null)
            .show()
        return false
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
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.background_color))
            chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
            chip.isChipIconVisible = false
        } else {
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.border_color))
            chip.setChipStrokeColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.border_color))
            chip.isChipIconVisible = true
        }
        if (chip.tag != null) {
            (chip.tag as ImageTag).isSelected = chip.isChecked
        }

        updateLicenseTextShown()
        callback().updateActionButton()
    }

    override fun onSearchSelect(item: ImageTag) {
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

        val acceptedLabels = mutableListOf<ImageTag>()
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

        EditAttemptStepEvent.logSaveAttempt(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)

        binding.publishProgressText.setText(R.string.suggested_edits_image_tags_publishing)
        binding.publishProgressCheck.visibility = GONE
        binding.publishOverlayContainer.visibility = VISIBLE
        binding.publishProgressBarComplete.visibility = GONE
        binding.publishProgressBar.visibility = VISIBLE

        disposables.add(CsrfTokenClient.getToken(Constants.commonsWikiSite)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ token ->
                    val mId = "M" + page!!.pageId
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
                    commentStr += " */" + DescriptionEditFragment.SUGGESTED_EDITS_IMAGE_TAGS_COMMENT

                    disposables.add(ServiceFactory.get(Constants.commonsWikiSite).postEditEntity(mId, token, claimStr, commentStr, null)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doAfterTerminate {
                                publishing = false
                            }
                            .subscribe({
                                if (it.entity != null) {
                                    EditAttemptStepEvent.logSaveSuccess(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                                }
                                publishSuccess = true
                                ImageRecommendationsEvent.logEditSuccess(DescriptionEditActivity.Action.ADD_IMAGE_TAGS,
                                    "commons", it.entity?.lastRevId ?: 0)
                                onSuccess()
                            }, {
                                onError(it)
                            })
                    )
                }, {
                    onError(it)
                }))
    }

    private fun onSuccess() {

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
        EditAttemptStepEvent.logSaveFailure(pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
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

    companion object {
        fun newInstance(): SuggestedEditsItemFragment {
            return SuggestedEditsImageTagsFragment()
        }
    }
}
