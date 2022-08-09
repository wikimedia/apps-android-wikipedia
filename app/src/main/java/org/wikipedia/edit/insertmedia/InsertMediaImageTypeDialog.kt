package org.wikipedia.edit.insertmedia

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogInsertMediaTypeBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class InsertMediaImageTypeDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onUpdateImageType()
    }

    private var _binding: DialogInsertMediaTypeBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel

    private lateinit var imageTypeOptions: Array<View>
    private val imageTypeList = arrayOf(
        InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL,
        InsertMediaViewModel.IMAGE_TYPE_FRAME,
        InsertMediaViewModel.IMAGE_TYPE_FRAMELESS,
        InsertMediaViewModel.IMAGE_TYPE_BASIC
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogInsertMediaTypeBinding.inflate(inflater, container, false)
        imageTypeOptions = arrayOf(binding.imageTypeThumbnail, binding.imageTypeFrame, binding.imageTypeFrameless, binding.imageTypeBasic)
        setupListeners()
        resetAllOptions()
        selectOption(viewModel.imageType)
        return binding.root
    }

    private fun setupListeners() {
        imageTypeOptions.forEachIndexed { index, view ->
            view.tag = imageTypeList[index]
            view.setOnClickListener(ImageTypeOptionClickListener())
        }
    }

    private fun selectOption(imageType: String) {
        imageTypeOptions.find { it.tag == imageType }?.let { updateOptionView(it, true) }
    }

    private fun resetAllOptions() {
        imageTypeOptions.forEach {
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

    inner class ImageTypeOptionClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            resetAllOptions()
            updateOptionView(view, true)
            viewModel.imageType = view.tag.toString()
            callback()?.onUpdateImageType()
            dismiss()
        }
    }

    companion object {
        fun newInstance(): InsertMediaImageTypeDialog {
            return InsertMediaImageTypeDialog()
        }
    }
}
