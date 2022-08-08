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
import org.wikipedia.util.log.L

class InsertMediaAdvancedSettingsFragment : Fragment(), InsertMediaImagePositionDialog.Callback {

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

        }

        binding.imageSizeButton.setOnClickListener {

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
        L.d("onSaveImagePosition called")
        L.d("onSaveImagePosition ${viewModel.imagePosition}")
        val newButtonText = when (viewModel.imagePosition) {
            InsertMediaViewModel.IMAGE_POSITION_RIGHT -> R.string.insert_media_advanced_settings_image_position_right
            InsertMediaViewModel.IMAGE_POSITION_CENTER -> R.string.insert_media_advanced_settings_image_position_center
            InsertMediaViewModel.IMAGE_POSITION_LEFT -> R.string.insert_media_advanced_settings_image_position_left
            else -> R.string.insert_media_advanced_settings_image_position_left
        }
        binding.imagePositionButton.text = getString(newButtonText)
    }
}
