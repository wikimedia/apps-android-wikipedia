package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogInsertMediaSizeBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class InsertMediaImageSizeDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun onSaveImageSize()
    }

    private var _binding: DialogInsertMediaSizeBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogInsertMediaSizeBinding.inflate(inflater, container, false)
        return binding.root
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
