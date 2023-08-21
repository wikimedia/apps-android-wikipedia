package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.databinding.FragmentInsertMediaAdvancedSettingsBinding
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.util.ResourceUtil

class InsertMediaAdvancedSettingsFragment : Fragment(), InsertMediaImagePositionDialog.Callback,
    InsertMediaImageTypeDialog.Callback, InsertMediaImageSizeDialog.Callback {

    private lateinit var activity: InsertMediaActivity
    private var _binding: FragmentInsertMediaAdvancedSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertMediaAdvancedSettingsBinding.inflate(layoutInflater, container, false)
        activity = (requireActivity() as InsertMediaActivity)

        binding.imagePositionButton.setOnClickListener {
            ExclusiveBottomSheetPresenter.show(childFragmentManager, InsertMediaImagePositionDialog.newInstance())
        }

        binding.imageTypeButton.setOnClickListener {
            ExclusiveBottomSheetPresenter.show(childFragmentManager, InsertMediaImageTypeDialog.newInstance())
        }

        binding.imageSizeButton.setOnClickListener {
            ExclusiveBottomSheetPresenter.show(childFragmentManager, InsertMediaImageSizeDialog.newInstance())
        }

        binding.wrapImageSwitch.isChecked = viewModel.imagePosition != InsertMediaViewModel.IMAGE_POSITION_NONE
        binding.wrapImageSwitch.setOnCheckedChangeListener { _, b -> onToggleWrapImage(b) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onUpdateImagePosition()
        onUpdateImageType()
        onUpdateImageSize()
    }

    private fun onToggleWrapImage(enabled: Boolean) {
        viewModel.imagePosition = if (enabled) InsertMediaViewModel.IMAGE_POSITION_RIGHT else InsertMediaViewModel.IMAGE_POSITION_NONE
        onUpdateImagePosition()
    }

    fun show() {
        binding.root.isVisible = true
        activity.invalidateOptionsMenu()
        activity.supportActionBar?.title = getString(R.string.insert_media_advanced_settings)
    }

    fun hide() {
        binding.root.isVisible = false
        activity.invalidateOptionsMenu()
    }

    fun handleBackPressed(): Boolean {
        if (isActive) {
            ImageRecommendationsEvent.logAction("advanced_setting_back", "caption_entry",
                ImageRecommendationsEvent.getActionDataString(filename = viewModel.selectedImage?.prefixedText.orEmpty(),
                    recommendationSource = viewModel.selectedImageSource), viewModel.wikiSite.languageCode)
            hide()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onUpdateImagePosition() {
        val newButtonText = when (viewModel.imagePosition) {
            InsertMediaViewModel.IMAGE_POSITION_RIGHT -> R.string.insert_media_advanced_settings_image_position_right
            InsertMediaViewModel.IMAGE_POSITION_CENTER -> R.string.insert_media_advanced_settings_image_position_center
            InsertMediaViewModel.IMAGE_POSITION_LEFT -> R.string.insert_media_advanced_settings_image_position_left
            else -> R.string.insert_media_advanced_settings_image_position_none
        }
        binding.imagePositionButton.isEnabled = viewModel.imagePosition != InsertMediaViewModel.IMAGE_POSITION_NONE
        binding.imagePositionButton.setTextColor(ResourceUtil.getThemedColor(requireContext(),
            if (binding.imagePositionButton.isEnabled) R.attr.progressive_color else R.attr.placeholder_color))
        binding.imagePositionButton.text = getString(newButtonText)
    }

    override fun onUpdateImageType() {
        val newButtonText = when (viewModel.imageType) {
            InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL -> R.string.insert_media_advanced_settings_image_type_thumbnail
            InsertMediaViewModel.IMAGE_TYPE_FRAME -> R.string.insert_media_advanced_settings_image_type_frame
            InsertMediaViewModel.IMAGE_TYPE_FRAMELESS -> R.string.insert_media_advanced_settings_image_type_frameless
            else -> R.string.insert_media_advanced_settings_image_type_basic
        }
        binding.imageTypeButton.text = getString(newButtonText)
    }

    override fun onUpdateImageSize() {
        val newButtonText = if (viewModel.imageSize == InsertMediaViewModel.IMAGE_SIZE_DEFAULT)
            R.string.insert_media_advanced_settings_image_size_default else R.string.insert_media_advanced_settings_image_size_custom
        binding.imageSizeButton.text = getString(newButtonText)
    }
}
