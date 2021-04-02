package org.wikipedia.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import org.wikipedia.BackPressedHandler
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.FragmentOnboardingPagerBinding

abstract class OnboardingFragment(val enableSkip: Boolean = true) : Fragment(), BackPressedHandler {
    interface Callback {
        fun onComplete()
    }

    private var _binding: FragmentOnboardingPagerBinding? = null
    private val binding get() = _binding!!
    private val pageChangeCallback = PageChangeCallback()

    private val forwardClickListener = View.OnClickListener {
        if (atLastPage()) {
            finish()
        } else {
            advancePage()
        }
    }

    protected abstract fun getAdapter(): FragmentStateAdapter?

    @get:StringRes
    protected abstract val doneButtonText: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnboardingPagerBinding.inflate(inflater, container, false)

        binding.fragmentPager.adapter = getAdapter()
        binding.fragmentPager.registerOnPageChangeCallback(pageChangeCallback)
        TabLayoutMediator(binding.viewOnboardingPageIndicator, binding.fragmentPager) { _, _ -> }.attach()
        binding.fragmentOnboardingDoneButton.setText(doneButtonText)

        if (savedInstanceState == null) {
            updateButtonState()
        }

        binding.fragmentOnboardingSkipButton.setOnClickListener {
            finish()
        }

        binding.fragmentOnboardingForwardButton.setOnClickListener(forwardClickListener)
        binding.fragmentOnboardingDoneButton.setOnClickListener(forwardClickListener)

        updatePageIndicatorContentDescription()
        return binding.root
    }

    override fun onDestroyView() {
        binding.fragmentPager.adapter = null
        binding.fragmentPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
        super.onDestroyView()
    }

    override fun onBackPressed(): Boolean {
        if (binding.fragmentPager.currentItem > 0) {
            binding.fragmentPager.setCurrentItem(binding.fragmentPager.currentItem - 1, true)
            return true
        }
        return false
    }

    fun advancePage() {
        if (!isAdded) {
            return
        }
        val nextPageIndex = binding.fragmentPager.currentItem + 1
        val lastPageIndex = binding.fragmentPager.adapter!!.itemCount - 1
        binding.fragmentPager.setCurrentItem(nextPageIndex.coerceAtMost(lastPageIndex), true)
    }

    protected fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    private fun finish() {
        callback()?.onComplete()
    }

    private fun atLastPage(): Boolean {
        return binding.fragmentPager.currentItem == binding.fragmentPager.adapter!!.itemCount - 1
    }

    private fun updatePageIndicatorContentDescription() {
        binding.viewOnboardingPageIndicator.contentDescription = getString(R.string.content_description_for_page_indicator,
                binding.fragmentPager.currentItem + 1, binding.fragmentPager.adapter!!.itemCount)
    }

    private fun updateButtonState() {
        if (atLastPage()) {
            binding.fragmentOnboardingSkipButton.visibility = View.GONE
            binding.fragmentOnboardingForwardButton.visibility = View.GONE
            binding.fragmentOnboardingDoneButton.visibility = View.VISIBLE
        } else {
            binding.fragmentOnboardingSkipButton.visibility = View.VISIBLE
            binding.fragmentOnboardingForwardButton.visibility = View.VISIBLE
            binding.fragmentOnboardingDoneButton.visibility = View.GONE
        }
    }

    private inner class PageChangeCallback : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateButtonState()
            updatePageIndicatorContentDescription()
            // TODO: request focus to child view to make it readable after switched page.
        }
    }
}
