package org.wikipedia.edit.summaries

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import org.wikipedia.databinding.FragmentPreviewSummaryBinding
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.ktx.windowInsetsControllerCompat
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil
import org.wikipedia.views.ViewAnimations

class EditSummaryFragment : Fragment() {
    private var _binding: FragmentPreviewSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var editSummaryHandler: EditSummaryHandler
    lateinit var title: PageTitle

    /**
     * Gets the custom ("other") summary, if any, that the user has entered.
     * @return Custom summary of the edit.
     */
    val summary get() = binding.editSummaryEdit.text.toString()
    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewSummaryBinding.inflate(layoutInflater, container, false)

        // Explicitly enable standard dictionary autocompletion in the edit summary box
        // We should be able to do this in the XML, but doing it there doesn't work. Thanks Android!
        binding.editSummaryEdit.inputType = binding.editSummaryEdit.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE.inv()

        // ...so that clicking the "Done" button on the keyboard will have the effect of
        // clicking the "Next" button in the actionbar:
        binding.editSummaryEdit.setOnEditorActionListener { _, actionId, keyEvent ->
            if ((keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) || actionId == EditorInfo.IME_ACTION_DONE) {
                (requireActivity() as EditSectionActivity).clickNextButton()
            }
            false
        }

        if (savedInstanceState != null) {
            binding.editSummaryEdit.setText(savedInstanceState.getString(KEY_SUMMARY_TEXT))
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        editSummaryHandler = EditSummaryHandler(binding.root, binding.editSummaryEdit, title)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /**
     * Shows (fades in) the custom edit summary fragment.
     * When fade-in completes, the keyboard is shown automatically, and the state
     * of the actionbar button(s) is updated.
     */
    fun show() {
        ViewAnimations.fadeIn(binding.root) {
            requireActivity().invalidateOptionsMenu()
            binding.editSummaryEdit.requestFocus()
            binding.editSummaryEdit.windowInsetsControllerCompat?.show(WindowInsetsCompat.Type.ime())
        }
    }

    /**
     * Hides (fades out) the custom edit summary fragment.
     * When fade-out completes, the keyboard is hidden, and the state of the actionbar
     * button(s) is updated.
     */
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SUMMARY_TEXT, binding.editSummaryEdit.text.toString())
    }

    /**
     * Commits the custom ("other") edit summary that the user may have entered,
     * so that it remains saved in a drop-down list for future use.
     */
    fun saveSummary() {
        if (binding.editSummaryEdit.length() > 0) {
            editSummaryHandler.persistSummary()
        }
    }

    companion object {
        private const val KEY_SUMMARY_TEXT = "summaryText"
    }
}
