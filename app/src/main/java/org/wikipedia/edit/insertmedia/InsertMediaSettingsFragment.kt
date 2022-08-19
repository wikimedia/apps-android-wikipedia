package org.wikipedia.edit.insertmedia

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentInsertMediaSettingsBinding
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ImagePreviewDialog
import org.wikipedia.views.ViewUtil

class InsertMediaSettingsFragment : Fragment() {

    private lateinit var activity: InsertMediaActivity
    private var _binding: FragmentInsertMediaSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = activity.viewModel
    private var currentVoiceInputParentLayout: View? = null
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    val isActive get() = binding.root.visibility == View.VISIBLE
    val alternativeText get() = binding.mediaAlternativeText.text.toString().trim()
    val captionText get() = binding.mediaCaptionText.text.toString().trim()

    private val voiceSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val voiceSearchResult = it.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (it.resultCode == Activity.RESULT_OK && voiceSearchResult != null) {
            val text = voiceSearchResult.first()
            if (currentVoiceInputParentLayout == binding.mediaCaptionLayout) {
                binding.mediaCaptionText.setText(text)
            } else if (currentVoiceInputParentLayout == binding.mediaAlternativeTextLayout) {
                binding.mediaAlternativeText.setText(text)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertMediaSettingsBinding.inflate(layoutInflater, container, false)
        activity = (requireActivity() as InsertMediaActivity)

        binding.mediaCaptionLayout.setEndIconOnClickListener {
            currentVoiceInputParentLayout = binding.mediaCaptionLayout
            launchVoiceInput()
        }
        binding.mediaAlternativeTextLayout.setEndIconOnClickListener {
            currentVoiceInputParentLayout = binding.mediaAlternativeTextLayout
            launchVoiceInput()
        }
        binding.advancedSettings.setOnClickListener {
            activity.showMediaAdvancedSettingsFragment()
        }
        binding.imageInfoContainer.setOnClickListener {
            viewModel.selectedImage?.let {
                val summary = PageSummaryForEdit(it.pageTitle.prefixedText, WikipediaApp.instance.appOrSystemLanguageCode, it.pageTitle,
                    it.pageTitle.displayText, RichTextUtil.stripHtml(it.imageInfo!!.metadata!!.imageDescription()), it.imageInfo.thumbUrl)
                bottomSheetPresenter.show(childFragmentManager,
                    ImagePreviewDialog.newInstance(summary))
            }
        }
        binding.mediaCaptionText.addTextChangedListener {
            if (!activity.isDestroyed) {
                activity.invalidateOptionsMenu()
            }
        }
        binding.mediaAlternativeText.addTextChangedListener {
            if (!activity.isDestroyed) {
                activity.invalidateOptionsMenu()
            }
        }
        return binding.root
    }

    private fun launchVoiceInput() {
        try {
            voiceSearchLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
        } catch (a: ActivityNotFoundException) {
            FeedbackUtil.showMessage(requireActivity(), R.string.error_voice_search_not_available)
        }
    }

    fun show() {
        binding.root.isVisible = true
        activity.invalidateOptionsMenu()
        activity.supportActionBar?.title = getString(R.string.insert_media_settings)
        viewModel.selectedImage?.let {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, it.pageTitle.thumbUrl, true)
            binding.mediaDescription.text = StringUtil.removeHTMLTags(it.imageInfo?.metadata?.imageDescription().orEmpty().ifEmpty { it.pageTitle.displayText })
        }
        binding.mediaCaptionLayout.requestFocus()
        DeviceUtil.showSoftKeyboard(binding.mediaCaptionText)
    }

    fun hide(reset: Boolean = true) {
        binding.root.isVisible = false
        activity.invalidateOptionsMenu()
        if (reset) {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, null)
            binding.mediaDescription.text = null
        }
    }

    fun handleBackPressed(): Boolean {
        if (isActive) {
            hide()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
