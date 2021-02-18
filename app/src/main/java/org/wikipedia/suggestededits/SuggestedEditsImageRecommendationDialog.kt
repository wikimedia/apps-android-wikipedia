package org.wikipedia.suggestededits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.FragmentSuggestedEditsImageRecommendationDialogBinding
import org.wikipedia.util.DimenUtil

class SuggestedEditsImageRecommendationDialog : DialogFragment() {
    interface Callback {
        fun onDialogSubmit()
    }

    private var _binding: FragmentSuggestedEditsImageRecommendationDialogBinding? = null
    private val binding get() = _binding!!

    private val checkBoxChangedHandler = CompoundButton.OnCheckedChangeListener { view, isChecked ->
        view.setBackgroundResource(if (isChecked) R.drawable.rounded_12dp_accent90_fill else R.drawable.rounded_12dp_corner_base90_fill)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuggestedEditsImageRecommendationDialogBinding.inflate(inflater, container, false)

        binding.checkBox1.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox2.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox3.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox4.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox5.setOnCheckedChangeListener(checkBoxChangedHandler)
        binding.checkBox6.setOnCheckedChangeListener(checkBoxChangedHandler)

        binding.continueButton.setOnClickListener {
            // TODO: pass selected state to consumer.
            callback()?.onDialogSubmit()
            dismiss()
        }

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

    fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }
}
