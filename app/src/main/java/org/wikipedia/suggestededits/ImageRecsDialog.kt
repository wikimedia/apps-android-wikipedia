package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.ImageRecommendationsFunnel
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecommendationDialogBinding
import org.wikipedia.util.DimenUtil

class ImageRecsDialog : DialogFragment() {
    interface Callback {
        fun onDialogSubmit(response: Int, selectedItems: List<Int>)
    }

    private var _binding: FragmentSuggestedEditsImageRecommendationDialogBinding? = null
    private val binding get() = _binding!!

    private var responseCode: Int = -1

    private val checkBoxChangedHandler = CompoundButton.OnCheckedChangeListener { view, isChecked ->
        view.setBackgroundResource(if (isChecked) R.drawable.rounded_12dp_accent90_fill else R.drawable.rounded_12dp_corner_base90_fill)
        updateSubmitState()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuggestedEditsImageRecommendationDialogBinding.inflate(inflater, container, false)
        responseCode = requireArguments().getInt(ARG_RESPONSE)

        binding.checkBox1.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox2.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox3.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox4.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox5.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox6.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox7.setOnCheckedChangeListener(checkBoxChangedHandler)

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.continueButton.setOnClickListener {
            val itemList = ArrayList<Int>()
            if (binding.checkBox1.isChecked) { itemList.add(0) }
            if (binding.checkBox2.isChecked) { itemList.add(1) }
            if (binding.checkBox3.isChecked) { itemList.add(2) }
            if (binding.checkBox4.isChecked) { itemList.add(3) }
            if (binding.checkBox5.isChecked) { itemList.add(4) }
            if (binding.checkBox6.isChecked) { itemList.add(5) }
            if (binding.checkBox7.isChecked) { itemList.add(5) }

            callback()?.onDialogSubmit(responseCode, itemList)
            dismiss()
        }

        if (responseCode == ImageRecommendationsFunnel.RESPONSE_REJECT) {
            binding.instructionText.text = getString(R.string.image_recommendations_task_choice_no)
            binding.checkBox1.text = getString(R.string.image_recommendations_task_choice_not_relevant)
            binding.checkBox2.text = getString(R.string.image_recommendations_task_choice_noinfo)
            binding.checkBox3.text = getString(R.string.image_recommendations_task_choice_offensive)
            binding.checkBox4.text = getString(R.string.image_recommendations_task_choice_low_quality)
            binding.checkBox5.text = getString(R.string.image_recommendations_task_choice_nosubject)
            binding.checkBox6.text = getString(R.string.image_recommendations_task_choice_language)
            binding.checkBox7.text = getString(R.string.image_recommendations_task_choice_other)
            binding.checkBox7.visibility = View.VISIBLE
        } else if (responseCode == ImageRecommendationsFunnel.RESPONSE_NOT_SURE) {
            binding.instructionText.text = getString(R.string.image_recommendations_task_choice_unsure)
            binding.checkBox1.text = getString(R.string.image_recommendations_task_choice_noinfo)
            binding.checkBox2.text = getString(R.string.image_recommendations_task_choice_invisible)
            binding.checkBox3.text = getString(R.string.image_recommendations_task_choice_nosubject)
            binding.checkBox4.text = getString(R.string.image_recommendations_task_choice_understand)
            binding.checkBox5.text = getString(R.string.image_recommendations_task_choice_language)
            binding.checkBox6.text = getString(R.string.image_recommendations_task_choice_other)
            binding.checkBox7.visibility = View.GONE
        }

        updateSubmitState()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.root.layoutParams = FrameLayout.LayoutParams(DimenUtil.displayWidthPx - (DimenUtil.roundedDpToPx(32f)), FrameLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateSubmitState() {
        val enabled = (binding.checkBox1.isChecked || binding.checkBox2.isChecked || binding.checkBox3.isChecked ||
                binding.checkBox4.isChecked || binding.checkBox5.isChecked || binding.checkBox6.isChecked ||
                binding.checkBox7.isChecked)
        binding.continueButton.isEnabled = enabled
        binding.continueButton.alpha = if (enabled) 1f else 0.5f
    }

    fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        const val ARG_RESPONSE = "response"

        fun newInstance(response: Int): ImageRecsDialog {
            return ImageRecsDialog().apply {
                arguments = bundleOf(ARG_RESPONSE to response)
            }
        }
    }
}
