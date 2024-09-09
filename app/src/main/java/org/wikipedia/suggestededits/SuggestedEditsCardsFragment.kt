package org.wikipedia.suggestededits

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.FragmentSuggestedEditsCardsBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS
import org.wikipedia.descriptions.DescriptionEditActivity.Action.IMAGE_RECOMMENDATIONS
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditActivity.Action.VANDALISM_PATROL
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.SuggestionsActivity.Companion.EXTRA_SOURCE_ADDED_CONTRIBUTION
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class SuggestedEditsCardsFragment : Fragment(), MenuProvider, SuggestedEditsItemFragment.Callback {
    private var _binding: FragmentSuggestedEditsCardsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SuggestedEditsCardsViewModel by viewModels { SuggestedEditsCardsViewModel.Factory(requireArguments()) }
    private val viewPagerListener = ViewPagerListener()
    private val app = WikipediaApp.instance
    private var swappingLanguageSpinners: Boolean = false
    private var resettingViewPager: Boolean = false

    private val topTitle: PageTitle?
        get() {
            val f = topChild()
            return if (viewModel.action == ADD_DESCRIPTION || viewModel.action == ADD_CAPTION) {
                f?.sourceSummaryForEdit?.pageTitle?.description = f?.addedContribution
                f?.sourceSummaryForEdit?.pageTitle
            } else {
                f?.targetSummaryForEdit?.pageTitle?.description = f?.addedContribution
                f?.targetSummaryForEdit?.pageTitle
            }
        }

    fun topBaseChild(): SuggestedEditsItemFragment? {
        return (binding.cardsViewPager.adapter as ViewPagerAdapter?)?.getFragmentAt(binding.cardsViewPager.currentItem) as SuggestedEditsItemFragment?
    }

    fun langFromCode(): String {
        return viewModel.langFromCode
    }

    fun langToCode(): String {
        return viewModel.langToCode
    }

    fun action(): DescriptionEditActivity.Action {
        return viewModel.action
    }

    private fun topChild(): SuggestedEditsCardsItemFragment? {
        return (binding.cardsViewPager.adapter as ViewPagerAdapter?)?.getFragmentAt(binding.cardsViewPager.currentItem) as SuggestedEditsCardsItemFragment?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSuggestedEditsCardsBinding.inflate(layoutInflater, container, false)

        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getActionBarTitle(intent.getSerializableExtra(Constants.INTENT_EXTRA_ACTION) as DescriptionEditActivity.Action)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        setInitialUiState()
        binding.cardsViewPager.offscreenPageLimit = 2
        binding.cardsViewPager.registerOnPageChangeCallback(viewPagerListener) // addOnPageChangeListener(viewPagerListener)
        resetViewPagerItemAdapter()

        if (viewModel.action == IMAGE_RECOMMENDATIONS) {
            binding.cardsViewPager.isUserInputEnabled = false
        }

        binding.backButton.setOnClickListener { previousPage() }
        binding.nextButton.setOnClickListener {
            if (binding.nextButton.drawable is Animatable) {
                (binding.nextButton.drawable as Animatable).start()
            }
            nextPage(null)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is Resource.Success -> onSuccess(it.data)
                    }
                }
            }
        }

        updateBackButton(0)
        binding.addContributionButton.setOnClickListener { onSelectPage() }
        updateActionButton()
        maybeShowOnboarding()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (viewModel.action == IMAGE_RECOMMENDATIONS || viewModel.action == VANDALISM_PATROL) {
            // In these cases, the sub-fragment will have its own menu.
            return
        }
        menuInflater.inflate(R.menu.menu_suggested_edits, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                when (viewModel.action) {
                    ADD_IMAGE_TAGS -> {
                        FeedbackUtil.showAndroidAppEditingFAQ(requireContext(),
                            R.string.suggested_edits_image_tags_help_url)
                    }
                    else -> {
                        FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                    }
                }
                val child = topBaseChild()
                if (child != null && child is SuggestedEditsImageRecsFragment) {
                    child.onInfoClicked()
                }
                true
            }
            else -> false
        }
    }

    private fun onSuccess(list: List<String>) {
        if (binding.wikiLanguageDropdownContainer.visibility == VISIBLE) {
            initLanguageSpinners(list)
            binding.wikiFromLanguageSpinner.onItemSelectedListener = OnFromSpinnerItemSelectedListener()
            binding.wikiToLanguageSpinner.onItemSelectedListener = OnToSpinnerItemSelectedListener()
            binding.arrow.setOnClickListener { binding.wikiFromLanguageSpinner.setSelection(binding.wikiToLanguageSpinner.selectedItemPosition) }
        }
    }

    private fun getActionBarTitle(action: DescriptionEditActivity.Action): String {
        return when (action) {
            ADD_IMAGE_TAGS -> getString(R.string.suggested_edits_tag_images)
            ADD_CAPTION, TRANSLATE_CAPTION -> getString(R.string.suggested_edits_caption_images)
            IMAGE_RECOMMENDATIONS -> ""
            VANDALISM_PATROL -> getString(R.string.suggested_edits_edit_patrol)
            else -> getString(R.string.suggested_edits_describe_articles)
        }
    }

    private fun maybeShowOnboarding() {
        if (viewModel.action == ADD_IMAGE_TAGS && Prefs.showImageTagsOnboarding) {
            Prefs.showImageTagsOnboarding = false
            startActivity(SuggestedEditsImageTagsOnboardingActivity.newIntent(requireContext()))
        } else if (viewModel.action == IMAGE_RECOMMENDATIONS && !Prefs.suggestedEditsImageRecsOnboardingShown) {
            startActivity(SuggestedEditsImageRecsOnboardingActivity.newIntent(requireActivity()))
        }
    }

    private fun updateBackButton(pagerPosition: Int) {
        binding.backButton.isClickable = pagerPosition != 0
        binding.backButton.alpha = if (pagerPosition == 0) 0.31f else 1f
    }

    override fun getLangCode(): String {
        return viewModel.langFromCode
    }

    override fun getSinglePage(): MwQueryPage? {
        return null
    }

    override fun updateActionButton() {
        val child = topBaseChild()
        var isAddedContributionEmpty = true
        if (child != null) {
            if (child is SuggestedEditsCardsItemFragment) {
                isAddedContributionEmpty = child.addedContribution.isEmpty()
                if (!isAddedContributionEmpty) child.showAddedContributionView(child.addedContribution)
            }
            binding.addContributionButton.setIconResource((if (isAddedContributionEmpty) R.drawable.ic_add_gray_white_24dp else R.drawable.ic_mode_edit_white_24dp))
            binding.addContributionButton.isEnabled = child.publishEnabled()
            binding.addContributionButton.alpha = if (child.publishEnabled()) 1f else 0.5f
        }

        binding.bottomButtonContainer.isVisible = viewModel.action != IMAGE_RECOMMENDATIONS

        if (viewModel.action == VANDALISM_PATROL) {
            binding.bottomButtonContainer.isVisible = false
        } else if (viewModel.action == ADD_IMAGE_TAGS) {
            if (binding.addContributionButton.tag == "landscape") {
                // implying landscape mode, where addContributionText doesn't exist.
                binding.addContributionButton.text = null
                binding.addContributionButton.setIconResource(R.drawable.ic_check_black_24dp)
            } else {
                binding.addContributionButton.text = getString(R.string.description_edit_save)
                binding.addContributionButton.icon = null
            }
        } else if (viewModel.action == TRANSLATE_DESCRIPTION || viewModel.action == TRANSLATE_CAPTION) {
            binding.addContributionButton.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_translation_button else R.string.suggested_edits_edit_translation_button)
        } else if (binding.addContributionButton.tag == "portrait") {
            if (viewModel.action == ADD_CAPTION) {
                binding.addContributionButton.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_caption_button else R.string.suggested_edits_edit_caption_button)
            } else {
                binding.addContributionButton.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_description_button else R.string.suggested_edits_edit_description_button)
            }
        }
    }

    override fun onDestroyView() {
        binding.cardsViewPager.unregisterOnPageChangeCallback(viewPagerListener)
        binding.cardsViewPager.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT && resultCode == RESULT_OK) {
            logSuccess()
            topChild()?.showAddedContributionView(data?.getStringExtra(EXTRA_SOURCE_ADDED_CONTRIBUTION))
            val targetLangCode = topChild()?.targetSummaryForEdit?.lang
            FeedbackUtil.showMessage(this,
                    when (viewModel.action) {
                        ADD_CAPTION -> getString(R.string.description_edit_success_saved_image_caption_snackbar)
                        TRANSLATE_CAPTION -> getString(R.string.description_edit_success_saved_image_caption_in_lang_snackbar, app.languageState.getAppLanguageLocalizedName(targetLangCode))
                        TRANSLATE_DESCRIPTION -> getString(R.string.description_edit_success_saved_in_lang_snackbar, app.languageState.getAppLanguageLocalizedName(targetLangCode))
                        else -> getString(R.string.description_edit_success_saved_snackbar)
                    }
            )
            nextPage(null)
        }
    }

    private fun previousPage() {
        viewPagerListener.setNextPageSelectedAutomatic()
        if (binding.cardsViewPager.currentItem > 0) {
            binding.cardsViewPager.setCurrentItem(binding.cardsViewPager.currentItem - 1, true)
        }
        updateActionButton()
    }

    override fun nextPage(sourceFragment: Fragment?) {
        if (sourceFragment == topBaseChild() || sourceFragment == null) {
            viewPagerListener.setNextPageSelectedAutomatic()
            binding.cardsViewPager.setCurrentItem(binding.cardsViewPager.currentItem + 1, true)
            updateActionButton()
        }
    }

    override fun logSuccess() {
    }

    fun onSelectPage() {
        if ((viewModel.action == ADD_IMAGE_TAGS || viewModel.action == VANDALISM_PATROL) && topBaseChild() != null) {
            topBaseChild()?.publish()
        } else if (viewModel.action == IMAGE_RECOMMENDATIONS) {
            topBaseChild()?.publish()
        } else if (topTitle != null) {
            startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), topTitle!!, null, topChild()?.sourceSummaryForEdit, topChild()?.targetSummaryForEdit,
                viewModel.action, Constants.InvokeSource.SUGGESTED_EDITS), Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT)
        }
    }

    private fun resetViewPagerItemAdapter() {
        if (!resettingViewPager) {
            resettingViewPager = true
            val postDelay: Long = 250
            binding.cardsViewPager.postDelayed({
                if (isAdded) {
                    binding.cardsViewPager.adapter = ViewPagerAdapter(this)
                    resettingViewPager = false
                }
            }, postDelay)
        }
    }

    private fun setInitialUiState() {
        binding.wikiLanguageDropdownContainer.visibility = if (app.languageState.appLanguageCodes.size > 1 &&
                (viewModel.action == TRANSLATE_DESCRIPTION || viewModel.action == TRANSLATE_CAPTION)) VISIBLE else GONE
    }

    private fun swapLanguageSpinnerSelection(isFromLang: Boolean) {
        if (!swappingLanguageSpinners) {
            swappingLanguageSpinners = true
            val preLangPosition = app.languageState.appLanguageCodes.indexOf(if (isFromLang) viewModel.langFromCode else viewModel.langToCode)
            if (isFromLang) {
                binding.wikiToLanguageSpinner.setSelection(preLangPosition)
            } else {
                binding.wikiFromLanguageSpinner.setSelection(preLangPosition)
            }
            swappingLanguageSpinners = false
        }
    }

    private fun initLanguageSpinners(list: List<String>) {
        binding.wikiFromLanguageSpinner.adapter = ArrayAdapter(requireContext(), R.layout.item_language_spinner, list)
        binding.wikiToLanguageSpinner.adapter = ArrayAdapter(requireContext(), R.layout.item_language_spinner, list)
        binding.wikiToLanguageSpinner.setSelection(app.languageState.appLanguageCodes.indexOf(viewModel.langToCode))
    }

    private inner class OnFromSpinnerItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (viewModel.langToCode == app.languageState.appLanguageCodes[position]) {
                swapLanguageSpinnerSelection(true)
            }

            if (!swappingLanguageSpinners && viewModel.langFromCode != app.languageState.appLanguageCodes[position]) {
                viewModel.langFromCode = app.languageState.appLanguageCodes[position]
                resetViewPagerItemAdapter()
                updateBackButton(0)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
        }
    }

    private inner class OnToSpinnerItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (viewModel.langFromCode == app.languageState.appLanguageCodes[position]) {
                swapLanguageSpinnerSelection(false)
            }

            if (!swappingLanguageSpinners && viewModel.langToCode != app.languageState.appLanguageCodes[position]) {
                viewModel.langToCode = app.languageState.appLanguageCodes[position]
                resetViewPagerItemAdapter()
                updateBackButton(0)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
        }
    }

    private inner class ViewPagerAdapter constructor(fragment: Fragment) : PositionAwareFragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return Integer.MAX_VALUE
        }
        override fun createFragment(position: Int): Fragment {
            return when (viewModel.action) {
                VANDALISM_PATROL -> {
                    SuggestedEditsVandalismPatrolFragment.newInstance()
                }
                ADD_IMAGE_TAGS -> {
                    SuggestedEditsImageTagsFragment.newInstance()
                }
                IMAGE_RECOMMENDATIONS -> {
                    SuggestedEditsImageRecsFragment.newInstance()
                }
                else -> {
                    SuggestedEditsCardsItemFragment.newInstance()
                }
            }
        }
    }

    private inner class ViewPagerListener : ViewPager2.OnPageChangeCallback() {
        private var prevPosition: Int = 0
        private var nextPageSelectedAutomatic: Boolean = false

        fun setNextPageSelectedAutomatic() {
            nextPageSelectedAutomatic = true
        }

        override fun onPageSelected(position: Int) {
            if (viewModel.action == IMAGE_RECOMMENDATIONS) {
                ((binding.cardsViewPager.adapter as ViewPagerAdapter?)?.getFragmentAt(position) as SuggestedEditsImageRecsFragment).logImpression()
            } else if (viewModel.action == VANDALISM_PATROL) {
                PatrollerExperienceEvent.logAction(if (position < prevPosition) "edit_left_swipe" else "edit_right_swipe", "pt_edit")
            }
            updateBackButton(position)
            updateActionButton()

            nextPageSelectedAutomatic = false
            prevPosition = position

            val storedOffScreenPagesCount = binding.cardsViewPager.offscreenPageLimit * 2 + 1
            if (position >= storedOffScreenPagesCount) {
                (binding.cardsViewPager.adapter as ViewPagerAdapter).removeFragmentAt(position - storedOffScreenPagesCount)
            }
        }
    }

    companion object {
        fun newInstance(action: DescriptionEditActivity.Action): SuggestedEditsCardsFragment {
            return SuggestedEditsCardsFragment().apply {
                arguments = bundleOf(
                    Constants.INTENT_EXTRA_ACTION to action,
                )
            }
        }
    }
}
