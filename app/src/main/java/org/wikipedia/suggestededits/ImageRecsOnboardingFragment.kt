package org.wikipedia.suggestededits

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.model.EnumCode
import org.wikipedia.model.EnumCodeMap
import org.wikipedia.onboarding.OnboardingFragment
import org.wikipedia.onboarding.OnboardingPageView
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil.showAboutWikipedia
import org.wikipedia.util.FeedbackUtil.showOfflineReadingAndData
import org.wikipedia.util.FeedbackUtil.showPrivacyPolicy
import org.wikipedia.util.UriUtil.handleExternalLink

class ImageRecsOnboardingFragment : OnboardingFragment(false), OnboardingPageView.Callback {

    override fun getAdapter(): FragmentStateAdapter {
        return OnboardingPagerAdapter(this)
    }

    override val doneButtonText = R.string.onboarding_get_started

    override fun onSwitchChange(view: OnboardingPageView, checked: Boolean) {
        if (OnboardingPage.of(view.tag as Int) == OnboardingPage.PAGE_CONSENT) {
            Prefs.setImageRecsConsentEnabled(checked)
        }
    }

    override fun onLinkClick(view: OnboardingPageView, url: String) {
        when (url) {
            "#privacy" -> showPrivacyPolicy(requireContext())
            "#about" -> showAboutWikipedia(requireContext())
            "#offline" -> showOfflineReadingAndData(requireContext())
            else -> handleExternalLink(requireActivity(), Uri.parse(url))
        }
    }

    override fun onListActionButtonClicked(view: OnboardingPageView) { }

    private class OnboardingPagerAdapter constructor(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment {
            return ItemFragment.newInstance(position)
        }

        override fun getItemCount(): Int {
            return OnboardingPage.size()
        }
    }

    class ItemFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            super.onCreateView(inflater, container, savedInstanceState)
            val position = requireArguments().getInt(ARG_POSITION, 0)
            val view = inflater.inflate(OnboardingPage.of(position).layout, container, false) as OnboardingPageView
            if (OnboardingPage.PAGE_CONSENT.code() == position) {
                view.setSwitchChecked(Prefs.isImageRecsConsentEnabled())
            }
            view.tag = position
            view.callback = callback
            return view
        }

        private val callback = getCallback(this, OnboardingPageView.Callback::class.java)

        companion object {
            const val ARG_POSITION = "position"

            fun newInstance(position: Int): ItemFragment {
                return ItemFragment().apply {
                    arguments = bundleOf(ARG_POSITION to position)
                }
            }
        }
    }

    @Suppress("unused")
    internal enum class OnboardingPage(@LayoutRes val layout: Int) : EnumCode {
        PAGE_WELCOME(R.layout.inflate_image_recs_onboarding_page_one),
        PAGE_CONSENT(R.layout.inflate_image_recs_onboarding_page_two);

        override fun code(): Int {
            return ordinal
        }

        companion object {
            private val MAP = EnumCodeMap(OnboardingPage::class.java)
            fun of(code: Int): OnboardingPage {
                return MAP[code]
            }

            fun size(): Int {
                return MAP.size()
            }
        }
    }

    companion object {
        fun newInstance(): ImageRecsOnboardingFragment {
            return ImageRecsOnboardingFragment()
        }
    }
}
