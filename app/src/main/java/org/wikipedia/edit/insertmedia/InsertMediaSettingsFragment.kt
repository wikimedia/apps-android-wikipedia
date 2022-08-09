package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.databinding.FragmentInsertMediaSettingsBinding
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class InsertMediaSettingsFragment : Fragment() {

    private lateinit var activity: InsertMediaActivity
    private var _binding: FragmentInsertMediaSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = activity.viewModel

    val isActive get() = binding.root.visibility == View.VISIBLE
    val alternativeText get() = binding.mediaAlternativeText.text.toString()
    val captionText get() = binding.mediaCaptionText.text.toString()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertMediaSettingsBinding.inflate(layoutInflater, container, false)
        activity = (requireActivity() as InsertMediaActivity)

        binding.advancedSettings.setOnClickListener {
            activity.showMediaAdvancedSettingsFragment()
        }
        return binding.root
    }

    fun show() {
        binding.root.isVisible = true
        activity.invalidateOptionsMenu()
        activity.supportActionBar?.title = getString(R.string.insert_media_settings)
        viewModel.selectedImage?.let {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, it.pageTitle.thumbUrl)
            binding.mediaDescription.text = StringUtil.removeHTMLTags(it.imageInfo?.metadata?.imageDescription().orEmpty().ifEmpty { it.pageTitle.displayText })
        }
    }

    fun hide(reset: Boolean = true) {
        binding.root.isVisible = false
        activity.invalidateOptionsMenu()
        if (reset) {
            ViewUtil.loadImageWithRoundedCorners(binding.imageView, null)
            binding.mediaDescription.text = null
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
