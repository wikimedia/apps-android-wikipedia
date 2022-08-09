package org.wikipedia.edit.insertmedia

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogInsertMediaSizeBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class InsertMediaImageSizeDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onUpdateImageSize()
    }

    private var _binding: DialogInsertMediaSizeBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogInsertMediaSizeBinding.inflate(inflater, container, false)
        setCustomSizeFields()
        binding.imageSizeCustomSwitch.isChecked = viewModel.imageSize != InsertMediaViewModel.IMAGE_SIZE_DEFAULT
        binding.imageSizeCustomHeightLayout.isEnabled = binding.imageSizeCustomSwitch.isChecked
        binding.imageSizeCustomWidthLayout.isEnabled = binding.imageSizeCustomSwitch.isChecked
        binding.imageSizeCustomSwitch.setOnCheckedChangeListener { _, b -> onToggleCustomSize(b) }
        return binding.root
    }

    private fun onToggleCustomSize(enabled: Boolean) {
        binding.imageSizeCustomHeightLayout.isEnabled = enabled
        binding.imageSizeCustomWidthLayout.isEnabled = enabled
        if (!enabled) {
            // Reset to default value
            viewModel.imageSize = InsertMediaViewModel.IMAGE_SIZE_DEFAULT
            setCustomSizeFields()
        }
    }

    private fun setCustomSizeFields() {
        val customImageSize = viewModel.imageSize.split("x")
        binding.imageSizeCustomWidthText.setText(customImageSize[0])
        binding.imageSizeCustomHeightText.setText(customImageSize[1])
    }

    override fun onDismiss(dialogInterface: DialogInterface) {
        super.onDismiss(dialogInterface)
        viewModel.imageSize = binding.imageSizeCustomWidthText.text.toString() + "x" + binding.imageSizeCustomHeightText.text.toString()
        callback()?.onUpdateImageSize()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(): InsertMediaImageSizeDialog {
            return InsertMediaImageSizeDialog()
        }
    }
}
