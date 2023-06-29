package org.wikipedia.suggestededits

import android.app.Dialog
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecsDialogBinding

class SuggestedEditsImageRecsDialog : DialogFragment() {
    interface Callback {
        fun onDialogSubmit(response: Int, selectedItems: List<Int>)
    }

    private var _binding: FragmentSuggestedEditsImageRecsDialogBinding? = null
    private val binding get() = _binding!!
    private var dialog: AlertDialog? = null

    private var responseCode = -1

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

        binding.checkBox1.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox2.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox3.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox4.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox5.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox6.setOnCheckedChangeListener(checkBoxChangedHandler)

        dialog = MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .setPositiveButton(R.string.image_recommendation_reject_submit) { _, _ ->
                val itemList = mutableListOf<Int>()
                if (binding.checkBox1.isChecked) { itemList.add(0) }
                if (binding.checkBox2.isChecked) { itemList.add(1) }
                if (binding.checkBox3.isChecked) { itemList.add(2) }
                if (binding.checkBox4.isChecked) { itemList.add(3) }
                if (binding.checkBox5.isChecked) { itemList.add(4) }
                if (binding.checkBox6.isChecked) { itemList.add(5) }

                callback()?.onDialogSubmit(responseCode, itemList)
                dismiss()
            }
            .setTitle(R.string.image_recommendation_reject_title)
            .create()
        return dialog!!
    }

    override fun onResume() {
        super.onResume()
        updateSubmitState()
    }

    private fun updateSubmitState() {
        val enabled = (binding.checkBox1.isChecked || binding.checkBox2.isChecked || binding.checkBox3.isChecked ||
                binding.checkBox4.isChecked || binding.checkBox5.isChecked || binding.checkBox6.isChecked)
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
