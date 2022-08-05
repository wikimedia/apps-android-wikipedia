package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.databinding.FragmentPreviewInsertMediaBinding
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class InsertMediaPreviewFragment : Fragment() {

    private var _binding: FragmentPreviewInsertMediaBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewInsertMediaBinding.inflate(layoutInflater, container, false)
        (requireActivity() as InsertMediaActivity).supportActionBar?.title = getString(R.string.insert_media_settings)

        return binding.root
    }

    fun show() {
        binding.root.isVisible = true
        requireActivity().invalidateOptionsMenu()
        viewModel.selectedImage?.let {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, it.pageTitle.thumbUrl)
            binding.mediaDescription.text = StringUtil.removeHTMLTags(it.imageInfo?.metadata?.imageDescription().orEmpty().ifEmpty { it.pageTitle.displayText })
        }
    }

    fun hide() {
        binding.root.isVisible = false
        requireActivity().invalidateOptionsMenu()
        ViewUtil.loadImageWithRoundedCorners(binding.imageView, null)
        binding.mediaDescription.text = null
    }

    fun handleBackPressed(): Boolean {
        if (isActive) {
            hide()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
