package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wikipedia.databinding.FragmentPreviewInsertMediaBinding
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.views.ViewAnimations

class InsertMediaPreviewFragment : Fragment() {

    private var _binding: FragmentPreviewInsertMediaBinding? = null
    private val binding get() = _binding!!
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewInsertMediaBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    fun show() {
        ViewAnimations.fadeIn(binding.root) {
            requireActivity().invalidateOptionsMenu()
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
