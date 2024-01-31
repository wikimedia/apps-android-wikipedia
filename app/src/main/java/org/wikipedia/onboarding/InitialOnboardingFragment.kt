package org.wikipedia.onboarding

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.login.LoginActivity
import org.wikipedia.model.EnumCode
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

class InitialOnboardingFragment : OnboardingFragment(), OnboardingPageView.Callback {
    private var onboardingPageView: OnboardingPageView? = null
    override val doneButtonText = R.string.onboarding_get_started
    override val showDoneButton = false

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
            advancePage()
        }
    }

    override fun getAdapter(): FragmentStateAdapter {
        return OnboardingPagerAdapter(this)
    }

    override fun onAcceptOrReject(view: OnboardingPageView, accept: Boolean) {
        if (OnboardingPage.of(view.tag as Int) == OnboardingPage.PAGE_USAGE_DATA) {
            advancePage()
        }
    }

    override fun onLinkClick(view: OnboardingPageView, url: String) {
        when (url) {
            "#login" -> loginLauncher.launch(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_ONBOARDING))
            "#privacy" -> FeedbackUtil.showPrivacyPolicy(requireContext())
            "#about" -> FeedbackUtil.showAboutWikipedia(requireContext())
            "#offline" -> FeedbackUtil.showOfflineReadingAndData(requireContext())
            else -> UriUtil.handleExternalLink(requireActivity(), Uri.parse(url))
        }
    }

    override fun onListActionButtonClicked(view: OnboardingPageView) {
        onboardingPageView = view
        requireContext().startActivity(WikipediaLanguagesActivity.newIntent(requireContext(), Constants.InvokeSource.ONBOARDING_DIALOG))
    }

    override fun onResume() {
        super.onResume()
        onboardingPageView?.refreshLanguageList()
    }

    private class OnboardingPagerAdapter constructor(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment {
            return ItemFragment.newInstance(position)
        }

        override fun getItemCount(): Int {
            return OnboardingPage.entries.size
        }
    }

    class ItemFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            super.onCreateView(inflater, container, savedInstanceState)
            val position = requireArguments().getInt("position", 0)
            val view = inflater.inflate(OnboardingPage.of(position).layout, container, false) as OnboardingPageView
            view.tag = position
            view.callback = callback
            return view
        }

        private val callback
            get() = FragmentUtil.getCallback(this, OnboardingPageView.Callback::class.java)

        companion object {
            fun newInstance(position: Int): ItemFragment {
                return ItemFragment().apply { arguments = bundleOf("position" to position) }
            }
        }
    }

    @Suppress("unused")
    internal enum class OnboardingPage(@LayoutRes val layout: Int) : EnumCode {
        PAGE_WELCOME(R.layout.inflate_initial_onboarding_page_zero),
        PAGE_EXPLORE(R.layout.inflate_initial_onboarding_page_one),
        PAGE_READING_LISTS(R.layout.inflate_initial_onboarding_page_two),
        PAGE_USAGE_DATA(R.layout.inflate_initial_onboarding_page_three);

        override fun code(): Int {
            return ordinal
        }

        companion object {
            fun of(code: Int): OnboardingPage {
                return entries[code]
            }
        }
    }

    companion object {
        fun newInstance(): InitialOnboardingFragment {
            return InitialOnboardingFragment()
        }
    }
}
