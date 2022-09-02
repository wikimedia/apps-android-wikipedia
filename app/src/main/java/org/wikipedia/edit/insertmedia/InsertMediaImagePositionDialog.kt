package org.wikipedia.edit.insertmedia

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogInsertMediaPositionBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class InsertMediaImagePositionDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onUpdateImagePosition()
    }

    private var _binding: DialogInsertMediaPositionBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel

    private lateinit var imagePositionOptions: Array<View>
    private val imagePositionList = arrayOf(
        InsertMediaViewModel.IMAGE_POSITION_RIGHT,
        InsertMediaViewModel.IMAGE_POSITION_CENTER,
        InsertMediaViewModel.IMAGE_POSITION_LEFT
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogInsertMediaPositionBinding.inflate(inflater, container, false)
        imagePositionOptions = arrayOf(binding.imagePositionRight, binding.imagePositionCenter, binding.imagePositionLeft)
        setupListeners()
        resetAllOptions()
        selectOption(viewModel.imagePosition)
        return binding.root
    }

    private fun setupListeners() {
        imagePositionOptions.forEachIndexed { index, view ->
            view.tag = imagePositionList[index]
            view.setOnClickListener(ImagePositionOptionClickListener())
        }
    }

    private fun selectOption(imagePosition: String) {
        imagePositionOptions.find { it.tag == imagePosition }?.let { updateOptionView(it, true) }
    }

    private fun resetAllOptions() {
        imagePositionOptions.forEach {
            updateOptionView(it, false)
        }
    }

    private fun updateOptionView(view: View, enabled: Boolean) {
        view.findViewWithTag<TextView>("text").typeface = if (enabled) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        view.findViewWithTag<ImageView>("check").visibility = if (enabled) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    inner class ImagePositionOptionClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            resetAllOptions()
            updateOptionView(view, true)
            viewModel.imagePosition = view.tag.toString()
            callback()?.onUpdateImagePosition()
            dismiss()
        }
    }

    companion object {
        fun newInstance(): InsertMediaImagePositionDialog {
            return InsertMediaImagePositionDialog()
        }
    }
}
