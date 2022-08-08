package org.wikipedia.edit.insertmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.databinding.FragmentInsertMediaAdvancedSettingsBinding
import org.wikipedia.page.ExclusiveBottomSheetPresenter

class InsertMediaAdvancedSettingsFragment : Fragment(), InsertMediaImagePositionDialog.Callback,
    InsertMediaImageTypeDialog.Callback, InsertMediaImageSizeDialog.Callback {

    private lateinit var activity: InsertMediaActivity
    private var _binding: FragmentInsertMediaAdvancedSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity() as InsertMediaActivity).viewModel
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertMediaAdvancedSettingsBinding.inflate(layoutInflater, container, false)
        activity = (requireActivity() as InsertMediaActivity)

        binding.imagePositionButton.setOnClickListener {
            bottomSheetPresenter.show(childFragmentManager, InsertMediaImagePositionDialog.newInstance())
        }

        binding.imageTypeButton.setOnClickListener {
            bottomSheetPresenter.show(childFragmentManager, InsertMediaImageTypeDialog.newInstance())
        }

        binding.imageSizeButton.setOnClickListener {
            bottomSheetPresenter.show(childFragmentManager, InsertMediaImageSizeDialog.newInstance())
        }

        return binding.root
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
            hide()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveImagePosition() {
        val newButtonText = when (viewModel.imagePosition) {
            InsertMediaViewModel.IMAGE_POSITION_RIGHT -> R.string.insert_media_advanced_settings_image_position_right
            InsertMediaViewModel.IMAGE_POSITION_CENTER -> R.string.insert_media_advanced_settings_image_position_center
            InsertMediaViewModel.IMAGE_POSITION_LEFT -> R.string.insert_media_advanced_settings_image_position_left
            else -> R.string.insert_media_advanced_settings_image_position_left
        }
        binding.imagePositionButton.text = getString(newButtonText)
    }

    override fun onSaveImageType() {
        val newButtonText = when (viewModel.imageType) {
            InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL -> R.string.insert_media_advanced_settings_image_type_thumbnail
            InsertMediaViewModel.IMAGE_TYPE_FRAME -> R.string.insert_media_advanced_settings_image_type_frame
            InsertMediaViewModel.IMAGE_TYPE_FRAMELESS -> R.string.insert_media_advanced_settings_image_type_frameless
            else -> R.string.insert_media_advanced_settings_image_type_basic
        }
        binding.imageTypeButton.text = getString(newButtonText)
    }

    override fun onSaveImageSize() {
        val newButtonText = if (viewModel.imageSize == InsertMediaViewModel.IMAGE_SIZE)
            R.string.insert_media_advanced_settings_image_size_default else R.string.insert_media_advanced_settings_image_size_custom
        binding.imageSizeButton.text = getString(newButtonText)
    }
}
