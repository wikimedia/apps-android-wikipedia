package org.wikipedia.navtab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ViewMainDrawerBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.places.PlacesActivity
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil.getThemedColorStateList

class MenuNavTabDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun usernameClick()
        fun loginClick()
        fun talkClick()
        fun settingsClick()
        fun watchlistClick()
        fun contribsClick()
        fun donateClick()
    }

    private var _binding: ViewMainDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ViewMainDrawerBinding.inflate(inflater, container, false)

        binding.mainDrawerAccountContainer.setOnClickListener {
            BreadCrumbLogEvent.logClick(requireActivity(), binding.mainDrawerAccountContainer)
            if (AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount) {
                callback()?.usernameClick()
            } else {
                callback()?.loginClick()
            }
            dismiss()
        }

        binding.mainDrawerTalkContainer.setOnClickListener {
            BreadCrumbLogEvent.logClick(requireActivity(), binding.mainDrawerTalkContainer)
            callback()?.talkClick()
            dismiss()
        }

        binding.mainDrawerWatchlistContainer.setOnClickListener {
            BreadCrumbLogEvent.logClick(requireActivity(), binding.mainDrawerWatchlistContainer)
            callback()?.watchlistClick()
            dismiss()
        }

        binding.mainDrawerPlacesContainer.setOnClickListener {
            PlacesEvent.logAction("places_click", "main_nav_tab")
            requireActivity().startActivity(PlacesActivity.newIntent(requireActivity()))
            dismiss()
        }

        binding.mainDrawerSettingsContainer.setOnClickListener {
            BreadCrumbLogEvent.logClick(requireActivity(), binding.mainDrawerSettingsContainer)
            callback()?.settingsClick()
            dismiss()
        }

        binding.mainDrawerContribsContainer.setOnClickListener {
            BreadCrumbLogEvent.logClick(requireActivity(), binding.mainDrawerContribsContainer)
            callback()?.contribsClick()
            dismiss()
        }

        binding.mainDrawerDonateContainer.setOnClickListener {
            DonorExperienceEvent.logAction("donate_start_click", "more_menu")
            BreadCrumbLogEvent.logClick(requireActivity(), binding.mainDrawerDonateContainer)
            callback()?.donateClick()
            dismiss()
        }

        updateState()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(binding.root.parent as View).peekHeight = DimenUtil.displayHeightPx
    }

    private fun updateState() {
        if (AccountUtil.isLoggedIn) {
            if (AccountUtil.isTemporaryAccount) {
                binding.mainDrawerAccountAvatar.setImageResource(R.drawable.ic_login_24px)
                ImageViewCompat.setImageTintList(binding.mainDrawerAccountAvatar, getThemedColorStateList(requireContext(), R.attr.progressive_color))
                binding.tempAccountName.text = AccountUtil.userName
                binding.mainDrawerAccountName.isVisible = false
                binding.mainDrawerLoginButton.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                binding.mainDrawerLoginButton.text = getString(R.string.main_drawer_login)
                binding.mainDrawerLoginButton.setTextColor(getThemedColorStateList(requireContext(), R.attr.progressive_color))
                binding.mainDrawerLoginButton.isVisible = true
            } else {
                binding.mainDrawerAccountAvatar.setImageResource(R.drawable.ic_baseline_person_24)
                ImageViewCompat.setImageTintList(binding.mainDrawerAccountAvatar, getThemedColorStateList(requireContext(), R.attr.secondary_color))
                binding.mainDrawerAccountName.text = AccountUtil.userName
                binding.mainDrawerAccountName.isVisible = true
                binding.mainDrawerLoginButton.isVisible = false
            }
            binding.mainDrawerTalkContainer.visibility = View.VISIBLE
            binding.mainDrawerTempAccountContainer.isVisible = AccountUtil.isTemporaryAccount
            binding.mainDrawerWatchlistContainer.isVisible = !AccountUtil.isTemporaryAccount
            binding.mainDrawerContribsContainer.visibility = View.VISIBLE
        } else {
            binding.mainDrawerAccountAvatar.setImageResource(R.drawable.ic_login_24px)
            ImageViewCompat.setImageTintList(binding.mainDrawerAccountAvatar, getThemedColorStateList(requireContext(), R.attr.progressive_color))
            binding.mainDrawerAccountName.visibility = View.GONE
            binding.mainDrawerTempAccountContainer.isVisible = false
            binding.mainDrawerLoginButton.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            binding.mainDrawerLoginButton.text = getString(R.string.main_drawer_login)
            binding.mainDrawerLoginButton.setTextColor(getThemedColorStateList(requireContext(), R.attr.progressive_color))
            binding.mainDrawerTalkContainer.visibility = View.GONE
            binding.mainDrawerWatchlistContainer.visibility = View.GONE
            binding.mainDrawerContribsContainer.visibility = View.GONE
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(): MenuNavTabDialog {
            return MenuNavTabDialog()
        }
    }
}
