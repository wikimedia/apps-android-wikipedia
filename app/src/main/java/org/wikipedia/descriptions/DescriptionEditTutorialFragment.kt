package org.wikipedia.descriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.onboarding.OnboardingFragment
import org.wikipedia.onboarding.OnboardingPageView

class DescriptionEditTutorialFragment : OnboardingFragment() {
    override val doneButtonText = R.string.description_edit_tutorial_button_label_start_editing
    override val showDoneButton = true
    var showAIOnBoarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAIOnBoarding = requireArguments().getBoolean(DescriptionEditTutorialActivity.SHOULD_SHOW_AI_ON_BOARDING)
    }

    override fun getAdapter(): FragmentStateAdapter {
        return DescriptionEditTutorialPagerAdapter(this)
    }

    internal inner class DescriptionEditTutorialPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return if (showAIOnBoarding) pages.size else pages.size - 1
        }

        override fun createFragment(position: Int): Fragment {
            return ItemFragment().apply { arguments = bundleOf(ARG_POSITION to position) }
        }
    }

    class ItemFragment() : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            super.onCreateView(inflater, container, savedInstanceState)
            val position = requireArguments().getInt(ARG_POSITION, 0)
            val showAIOnBoarding = requireParentFragment().requireArguments().getBoolean(DescriptionEditTutorialActivity.SHOULD_SHOW_AI_ON_BOARDING)
            val view = inflater.inflate(pages[position], container, false) as OnboardingPageView
            if (showAIOnBoarding) {
                if (position == 1) {
                    view.setTertiaryTextViewVisible(false)
                }
            }
            if (position == 2) {
                MachineGeneratedArticleDescriptionsAnalyticsHelper().logOnboardingShown(requireContext())
            }
            view.callback = OnboardingPageView.DefaultCallback()
            return view
        }
    }

    companion object {
        const val ARG_POSITION = "position"
        val pages = arrayOf(R.layout.inflate_description_edit_tutorial_page_one, R.layout.inflate_description_edit_tutorial_page_two, R.layout.inflate_description_edit_tutorial_page_three)

        fun newInstance(showAIOnBoarding: Boolean) = DescriptionEditTutorialFragment().apply {
            arguments = bundleOf(DescriptionEditTutorialActivity.SHOULD_SHOW_AI_ON_BOARDING to showAIOnBoarding)
        }
    }
}
