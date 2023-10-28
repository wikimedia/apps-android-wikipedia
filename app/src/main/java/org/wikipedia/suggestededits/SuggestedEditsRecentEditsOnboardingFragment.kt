package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.onboarding.OnboardingFragment
import org.wikipedia.onboarding.OnboardingPageView
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

class SuggestedEditsRecentEditsOnboardingFragment : OnboardingFragment(), OnboardingPageView.Callback {
    override val doneButtonText = R.string.onboarding_get_started
    override val showDoneButton = Prefs.isEventLoggingEnabled

    override fun getAdapter(): FragmentStateAdapter {
        return DescriptionEditTutorialPagerAdapter(this)
    }

    internal inner class DescriptionEditTutorialPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return if (!Prefs.isEventLoggingEnabled) pages.size else pages.size - 1
        }

        override fun createFragment(position: Int): Fragment {
            return ItemFragment().apply { arguments = bundleOf(ARG_POSITION to position) }
        }
    }

    override fun onAcceptOrReject(view: OnboardingPageView, accept: Boolean) {
        if ((view.tag as Int) == 2) {
            Prefs.isEventLoggingEnabled = accept
            requireActivity().finish()
        }
    }

    override fun onLinkClick(view: OnboardingPageView, url: String) {
        when (url) {
            "#privacy" -> FeedbackUtil.showPrivacyPolicy(requireContext())
            else -> UriUtil.handleExternalLink(requireActivity(), Uri.parse(url))
        }
    }

    override fun onListActionButtonClicked(view: OnboardingPageView) { }

    class ItemFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            super.onCreateView(inflater, container, savedInstanceState)
            val position = requireArguments().getInt(ARG_POSITION, 0)
            val view = inflater.inflate(pages[position], container, false) as OnboardingPageView
            view.tag = position
            view.callback = callback
            val action = if (position == 0) "funnel_init" else "funnel_${position}_advance"
            PatrollerExperienceEvent.logAction(action, "pt_onboarding_funnel")
            return view
        }

        private val callback
            get() = FragmentUtil.getCallback(this, OnboardingPageView.Callback::class.java)
    }

    companion object {
        const val ARG_POSITION = "position"
        val pages = arrayOf(
            R.layout.inflate_patroller_tasks_onboarding_page_one,
            R.layout.inflate_patroller_tasks_onboarding_page_two,
            R.layout.inflate_initial_onboarding_page_three
        )
        fun newInstance() = SuggestedEditsRecentEditsOnboardingFragment()
    }
}
