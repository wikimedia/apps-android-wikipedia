package org.wikipedia.navtab

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.NotificationsABCTestFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ViewMainDrawerBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil.getDimension
import org.wikipedia.util.DimenUtil.roundedDpToPx
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.UriUtil.visitInExternalBrowser

class MenuNavTabDialog : ExtendedBottomSheetDialogFragment() {
    interface Callback {
        fun usernameClick()
        fun loginClick()
        fun notificationsClick()
        fun talkClick()
        fun settingsClick()
        fun watchlistClick()
    }

    private var _binding: ViewMainDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ViewMainDrawerBinding.inflate(inflater, container, false)

        binding.mainDrawerAccountContainer.setOnClickListener {
            if (AccountUtil.isLoggedIn) {
                callback()?.usernameClick()
            } else {
                callback()?.loginClick()
            }
            dismiss()
        }

        binding.mainDrawerNotificationsContainer.setOnClickListener {
            callback()?.notificationsClick()
            dismiss()
        }

        binding.mainDrawerTalkContainer.setOnClickListener {
            callback()?.talkClick()
            dismiss()
        }

        binding.mainDrawerWatchlistContainer.setOnClickListener {
            callback()?.watchlistClick()
            dismiss()
        }

        binding.mainDrawerSettingsContainer.setOnClickListener {
            callback()?.settingsClick()
            dismiss()
        }

        binding.mainDrawerDonateContainer.setOnClickListener {
            visitInExternalBrowser(requireContext(),
                    Uri.parse(getString(R.string.donate_url,
                            BuildConfig.VERSION_NAME, WikipediaApp.getInstance().language().systemLanguageCode)))
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(binding.root.parent as View).peekHeight = roundedDpToPx(getDimension(R.dimen.navTabDialogPeekHeight))
    }

    private fun updateState() {
        if (AccountUtil.isLoggedIn) {
            binding.mainDrawerAccountAvatar.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_person_24))
            ImageViewCompat.setImageTintList(binding.mainDrawerAccountAvatar, ColorStateList.valueOf(getThemedColor(requireContext(), R.attr.material_theme_secondary_color)))
            binding.mainDrawerAccountName.text = AccountUtil.userName
            binding.mainDrawerAccountName.visibility = View.VISIBLE
            binding.mainDrawerLoginButton.visibility = View.GONE
            binding.mainDrawerLoginOpenExternalIcon.visibility = View.VISIBLE
            binding.mainDrawerTalkContainer.visibility = View.VISIBLE
            binding.mainDrawerWatchlistContainer.visibility = View.VISIBLE

            if (NotificationsABCTestFunnel().aBTestGroup > 1) {
                binding.mainDrawerNotificationsContainer.isVisible = true
                if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
                    binding.unreadDotView.setUnreadCount(Prefs.notificationUnreadCount)
                    binding.unreadDotView.isVisible = true
                } else {
                    binding.unreadDotView.isVisible = false
                    binding.unreadDotView.setUnreadCount(0)
                }
            } else {
                binding.mainDrawerNotificationsContainer.isVisible = false
                binding.unreadDotView.isVisible = false
            }
        } else {
            binding.mainDrawerAccountAvatar.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_login_24px))
            ImageViewCompat.setImageTintList(binding.mainDrawerAccountAvatar, ColorStateList.valueOf(getThemedColor(requireContext(), R.attr.colorAccent)))
            binding.mainDrawerAccountName.visibility = View.GONE
            binding.mainDrawerLoginButton.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            binding.mainDrawerLoginButton.text = getString(R.string.main_drawer_login)
            binding.mainDrawerLoginButton.setTextColor(getThemedColor(requireContext(), R.attr.colorAccent))
            binding.mainDrawerLoginOpenExternalIcon.visibility = View.GONE
            binding.mainDrawerNotificationsContainer.visibility = View.GONE
            binding.mainDrawerTalkContainer.visibility = View.GONE
            binding.mainDrawerWatchlistContainer.visibility = View.GONE
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        @JvmStatic
        fun newInstance(): MenuNavTabDialog {
            return MenuNavTabDialog()
        }
    }
}
