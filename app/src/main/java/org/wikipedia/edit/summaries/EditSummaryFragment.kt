package org.wikipedia.edit.summaries

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentPreviewSummaryBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewAnimations

class EditSummaryFragment : Fragment() {
    private var _binding: FragmentPreviewSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var editSummaryHandler: EditSummaryHandler
    lateinit var title: PageTitle

    val summaryText get() = binding.editSummaryText
    val summary get() = binding.editSummaryText.text.toString()
    val isMinorEdit get() = binding.minorEditCheckBox.isChecked
    val watchThisPage get() = binding.watchPageCheckBox.isChecked
    val isActive get() = binding.root.visibility == View.VISIBLE

    private val voiceSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val voiceSearchResult = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (it.resultCode == Activity.RESULT_OK && voiceSearchResult != null) {
            val text = voiceSearchResult.first()
            binding.editSummaryText.setText(text)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewSummaryBinding.inflate(layoutInflater, container, false)

        // Explicitly enable standard dictionary autocompletion in the edit summary box
        // We should be able to do this in the XML, but doing it there doesn't work. Thanks Android!
        binding.editSummaryText.inputType = binding.editSummaryText.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE.inv()

        // ...so that clicking the "Done" button on the keyboard will have the effect of
        // clicking the "Next" button in the actionbar:
        binding.editSummaryText.setOnEditorActionListener { _, actionId, keyEvent ->
            if ((keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) || actionId == EditorInfo.IME_ACTION_DONE) {
                (requireActivity() as EditSectionActivity).clickNextButton()
            }
            false
        }

        binding.editSummaryText.addTextChangedListener {
            if (!requireActivity().isDestroyed) {
                requireActivity().invalidateOptionsMenu()
            }
        }

        binding.editSummaryTextLayout.setEndIconOnClickListener {
            if ((requireActivity() as EditSectionActivity).invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                ImageRecommendationsEvent.logAction("tts_open", "editsummary_dialog", "", "")
            }
            launchVoiceInput()
        }

        binding.learnMoreButton.setOnClickListener {
            if ((requireActivity() as EditSectionActivity).invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                ImageRecommendationsEvent.logAction("view_help", "editsummary_dialog", "", "")
            }
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.meta_edit_summary_url)))
        }

        binding.minorEditHelpButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.meta_minor_edit_url)))
        }

        binding.watchPageHelpButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.meta_watching_pages_url)))
        }

        binding.watchPageCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if ((requireActivity() as EditSectionActivity).invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                ImageRecommendationsEvent.logAction(if (isChecked) "add_watchlist" else "remove_watchlist",
                    "editsummary_dialog", "", "")
            }
        }

        getWatchedStatus()

        addEditSummaries()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        editSummaryHandler = EditSummaryHandler(binding.root, binding.editSummaryText, title)
        if ((requireActivity() as EditSectionActivity).invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
            ImageRecommendationsEvent.logImpression("editsummary_dialog")
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun launchVoiceInput() {
        try {
            voiceSearchLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
        } catch (a: ActivityNotFoundException) {
            FeedbackUtil.showMessage(requireActivity(), R.string.error_voice_search_not_available)
        }
    }

    private fun getWatchedStatus() {
        if (AccountUtil.isLoggedIn) {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.e(throwable)
            }) {
                val query = ServiceFactory.get(title.wikiSite)
                    .getWatchedStatusWithUserOptions(title.prefixedText).query!!
                binding.watchPageCheckBox.isChecked = query.firstPage()!!.watched ||
                        query.userInfo?.options?.watchDefault == 1
            }
        } else {
            binding.watchPageCheckBox.isEnabled = false
            binding.watchPageCheckBox.alpha = 0.5f
        }
    }

    private fun addEditSummaries() {
        val summaryTagStrings = if ((requireActivity() as EditSectionActivity).invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE)
            intArrayOf(R.string.edit_summary_added_image_and_caption, R.string.edit_summary_added_image)
        else
            intArrayOf(R.string.edit_summary_tag_typo, R.string.edit_summary_tag_grammar, R.string.edit_summary_tag_links)

        val localizedSummaries = L10nUtil.getStringsForArticleLanguage(title, summaryTagStrings)
        summaryTagStrings.forEach {
            addChip(localizedSummaries[it])
        }
    }

    private fun addChip(editSummary: String): Chip {
        val chip = Chip(requireContext())
        chip.text = editSummary
        TextViewCompat.setTextAppearance(chip, R.style.Chip_Accessible)
        chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.background_color))
        chip.chipStrokeWidth = DimenUtil.dpToPx(1f)
        chip.chipStrokeColor = ColorStateList.valueOf(ResourceUtil.getThemedColor(requireContext(), R.attr.border_color))
        chip.shapeAppearanceModel = chip.shapeAppearanceModel.withCornerSize(DimenUtil.dpToPx(8f))
        chip.setCheckedIconResource(R.drawable.ic_chip_check_24px)
        chip.setOnClickListener {
            // Clear the text field and insert the text
            binding.editSummaryText.setText(editSummary)
            binding.editSummaryText.setSelection(editSummary.length)
        }

        // add some padding to the Chip, since our container view doesn't support item spacing yet.
        val params = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = DimenUtil.roundedDpToPx(4f)
        params.setMargins(margin, 0, margin, 0)
        chip.layoutParams = params

        binding.editSummaryTagsContainer.addView(chip)
        return chip
    }

    fun show() {
        ViewAnimations.fadeIn(binding.root) {
            requireActivity().invalidateOptionsMenu()
            binding.editSummaryText.requestFocus()
            DeviceUtil.showSoftKeyboard(binding.editSummaryText)
        }
    }

    fun hide() {
        ViewAnimations.fadeOut(binding.root) {
            DeviceUtil.hideSoftKeyboard(requireActivity())
            requireActivity().invalidateOptionsMenu()
        }
    }

    fun handleBackPressed(): Boolean {
        if (isActive) {
            hide()
            return editSummaryHandler.handleBackPressed()
        }
        return false
    }

    fun saveSummary() {
        if (binding.editSummaryText.length() > 0) {
            editSummaryHandler.persistSummary()
        }
    }
}
