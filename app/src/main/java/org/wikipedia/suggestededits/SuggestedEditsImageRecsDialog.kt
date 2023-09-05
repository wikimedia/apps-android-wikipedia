package org.wikipedia.suggestededits

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecsDialogBinding

class SuggestedEditsImageRecsDialog : DialogFragment() {
    interface Callback {
        fun onDialogSubmit(response: Int, selectedItems: List<Int>)
    }

    private var _binding: FragmentSuggestedEditsImageRecsDialogBinding? = null
    private val binding get() = _binding!!
    private var dialog: AlertDialog? = null
    private var responseCode = -1

    private lateinit var checkboxes: Array<CheckBox>

    private val checkBoxChangedHandler = CompoundButton.OnCheckedChangeListener { _, _ ->
        updateSubmitState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        responseCode = requireArguments().getInt(ARG_RESPONSE)
        _binding = FragmentSuggestedEditsImageRecsDialogBinding.inflate(layoutInflater)

        checkboxes = arrayOf(binding.checkBox1, binding.checkBox2, binding.checkBox3, binding.checkBox4, binding.checkBox5, binding.checkBox6)
        checkboxes.forEach { it.setOnCheckedChangeListener(checkBoxChangedHandler) }

        dialog = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                ImageRecommendationsEvent.logAction("reject_cancel", "rejection_dialog",
                    ImageRecommendationsEvent.getActionDataString(acceptanceState = "rejected"))
                dismiss() }
            .setPositiveButton(R.string.image_recommendation_reject_submit) { _, _ ->
                val itemList = checkboxes.mapIndexedNotNull { i, checkBox -> if (checkBox.isChecked) i else null }
                callback()?.onDialogSubmit(responseCode, itemList)
                dismiss()
            }
            .setTitle(R.string.image_recommendation_reject_title)
            .create()
        ImageRecommendationsEvent.logImpression("rejection_dialog")
        return dialog!!
    }

    override fun onResume() {
        super.onResume()
        updateSubmitState()
    }

    private fun updateSubmitState() {
        val enabled = checkboxes.any { it.isChecked }
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.alpha = if (enabled) 1f else 0.5f
    }

    fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        const val ARG_RESPONSE = "response"

        fun newInstance(response: Int): SuggestedEditsImageRecsDialog {
            return SuggestedEditsImageRecsDialog().apply {
                arguments = bundleOf(ARG_RESPONSE to response)
            }
        }
    }
}
