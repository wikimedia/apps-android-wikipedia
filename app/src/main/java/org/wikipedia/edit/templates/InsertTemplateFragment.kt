package org.wikipedia.edit.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.wikipedia.databinding.FragmentInsertTemplateBinding

class InsertTemplateFragment : Fragment() {

    private lateinit var activity: TemplatesSearchActivity
    private var _binding: FragmentInsertTemplateBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = activity.viewModel

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertTemplateBinding.inflate(layoutInflater, container, false)
        activity = (requireActivity() as TemplatesSearchActivity)

        // TODO: check all required params
//        binding.mediaAlternativeText.addTextChangedListener {
//            if (!activity.isDestroyed) {
//                activity.invalidateOptionsMenu()
//            }
//        }
        return binding.root
    }

    fun show() {
        binding.root.isVisible = true
        activity.invalidateOptionsMenu()
        // TODO: add input field focus?
    }

    fun hide() {
        binding.root.isVisible = false
        activity.invalidateOptionsMenu()
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
