package org.wikipedia.edit.summaries

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.FragmentPreviewSummaryBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewAnimations

class EditSummaryFragment : Fragment() {
    private var _binding: FragmentPreviewSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var editSummaryHandler: EditSummaryHandler
    lateinit var title: PageTitle

    val summary get() = binding.editSummaryText.text.toString()
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

        binding.editSummaryTextLayout.setEndIconOnClickListener {
            launchVoiceInput()
        }

        binding.learnMoreButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.preview_edit_learn_more_url)))
        }

        binding.minorEditHelpButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.preview_edit_minor_edit_url)))
        }

        binding.watchPageHelpButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.preview_edit_watch_this_page_url)))
        }

        getWatchedStatus()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        editSummaryHandler = EditSummaryHandler(binding.root, binding.editSummaryText, title)
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
                withContext(Dispatchers.IO) {
                    val page = ServiceFactory.get(title.wikiSite)
                        .getWatchedStatus(title.prefixedText).query?.firstPage()!!
                    binding.watchPageCheckBox.isChecked = page.watched
                }
            }
        } else {
            binding.watchPageCheckBox.isEnabled = false
        }
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
