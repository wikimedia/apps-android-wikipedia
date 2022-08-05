package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wikipedia.databinding.FragmentPreviewInsertMediaBinding
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewAnimations
import org.wikipedia.views.ViewUtil

class InsertMediaPreviewFragment : Fragment() {

    private var _binding: FragmentPreviewInsertMediaBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewInsertMediaBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    fun show() {
        ViewAnimations.fadeIn(binding.root) {
            requireActivity().invalidateOptionsMenu()
            viewModel.selectedImage?.let {
                ViewUtil.loadImageWithRoundedCorners(binding.imageView, it.pageTitle.thumbUrl)
                binding.mediaDescription.text = StringUtil.removeHTMLTags(it.imageInfo?.metadata?.imageDescription().orEmpty().ifEmpty { it.pageTitle.displayText })
            }
        }
    }

    fun hide() {
        ViewAnimations.fadeOut(binding.root) {
            requireActivity().invalidateOptionsMenu()
        }
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
