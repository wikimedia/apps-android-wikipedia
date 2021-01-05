package org.wikipedia.suggestededits

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_cards.*
import org.wikipedia.Constants.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.SuggestedEditsFeedFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Action.*
import org.wikipedia.suggestededits.SuggestionsActivity.Companion.EXTRA_SOURCE_ADDED_CONTRIBUTION
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class SuggestedEditsCardsFragment : Fragment(), SuggestedEditsImageTagsFragment.Callback {
    private val viewPagerListener = ViewPagerListener()
    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    private var siteMatrix: SiteMatrix? = null
    private var languageList: MutableList<String> = mutableListOf()
    private var swappingLanguageSpinners: Boolean = false
    private var resettingViewPager: Boolean = false
    private var funnel: SuggestedEditsFeedFunnel? = null

    var langFromCode: String = app.language().appLanguageCode
    var langToCode: String = if (app.language().appLanguageCodes.size == 1) "" else app.language().appLanguageCodes[1]
    var action: DescriptionEditActivity.Action = ADD_DESCRIPTION

    private val topTitle: PageTitle?
        get() {
            val f = topChild()
            return if (action == ADD_DESCRIPTION || action == ADD_CAPTION) {
                f?.sourceSummaryForEdit?.pageTitle?.description = f?.addedContribution
                f?.sourceSummaryForEdit?.pageTitle
            } else {
                f?.targetSummaryForEdit?.pageTitle?.description = f?.addedContribution
                f?.targetSummaryForEdit?.pageTitle
            }
        }

    private fun topBaseChild(): SuggestedEditsItemFragment? {
        return (cardsViewPager.adapter as ViewPagerAdapter?)?.getFragmentAt(cardsViewPager.currentItem) as SuggestedEditsItemFragment?
    }

    private fun topChild(): SuggestedEditsCardsItemFragment? {
        return (cardsViewPager.adapter as ViewPagerAdapter?)?.getFragmentAt(cardsViewPager.currentItem) as SuggestedEditsCardsItemFragment?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        action = arguments?.getSerializable(INTENT_EXTRA_ACTION) as DescriptionEditActivity.Action

        funnel = SuggestedEditsFeedFunnel(action, requireArguments().getSerializable(INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource)

        // Record the first impression, since the ViewPager doesn't send an event for the first topmost item.
        SuggestedEditsFunnel.get().impression(action)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_cards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialUiState()
        cardsViewPager.offscreenPageLimit = 2
        cardsViewPager.registerOnPageChangeCallback(viewPagerListener)//   addOnPageChangeListener(viewPagerListener)
        resetViewPagerItemAdapter()

        funnel?.start()

        if (wikiLanguageDropdownContainer.visibility == VISIBLE) {
            if (languageList.isEmpty()) {
                // Fragment is created for the first time.
                requestLanguagesAndBuildSpinner()
            } else {
                // Fragment already exists, so just update the UI.
                initLanguageSpinners()
            }
            wikiFromLanguageSpinner.onItemSelectedListener = OnFromSpinnerItemSelectedListener()
            wikiToLanguageSpinner.onItemSelectedListener = OnToSpinnerItemSelectedListener()
            arrow.setOnClickListener { wikiFromLanguageSpinner.setSelection(wikiToLanguageSpinner.selectedItemPosition) }
        }

        backButton.setOnClickListener { previousPage() }
        nextButton.setOnClickListener {
            if (nextButton.drawable is Animatable) {
                (nextButton.drawable as Animatable).start()
            }
            nextPage(null)
        }
        updateBackButton(0)
        addContributionButton.setOnClickListener { onSelectPage() }
        updateActionButton()
        maybeShowOnboarding()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(action == ADD_IMAGE_TAGS)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (action == ADD_IMAGE_TAGS) {
            inflater.inflate(R.menu.menu_suggested_edits, menu)
            ResourceUtil.setMenuItemTint(requireContext(), menu.findItem(R.id.menu_help), R.attr.colorAccent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                if (action == ADD_IMAGE_TAGS) {
                    FeedbackUtil.showAndroidAppEditingFAQ(requireContext(), R.string.suggested_edits_image_tags_help_url)
                } else {
                    FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun maybeShowOnboarding() {
        if (action == ADD_IMAGE_TAGS && Prefs.shouldShowImageTagsOnboarding()) {
            Prefs.setShowImageTagsOnboarding(false)
            startActivity(SuggestedEditsImageTagsOnboardingActivity.newIntent(requireContext()))
        }
    }

    private fun updateBackButton(pagerPosition: Int) {
        backButton.isClickable = pagerPosition != 0
        backButton.alpha = if (pagerPosition == 0) 0.31f else 1f
    }

    override fun getLangCode(): String {
        return langFromCode
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
            addContributionButton.setIconResource((if (isAddedContributionEmpty) R.drawable.ic_add_gray_white_24dp else R.drawable.ic_mode_edit_white_24dp))
            addContributionButton.isEnabled = child.publishEnabled()
            addContributionButton.alpha = if (child.publishEnabled()) 1f else 0.5f
        }

        if (action == ADD_IMAGE_TAGS) {
            if (addContributionButton.tag == "landscape") {
                // implying landscape mode, where addContributionText doesn't exist.
                addContributionButton.text = null
                addContributionButton.setIconResource(R.drawable.ic_check_black_24dp)
            } else {
                addContributionButton.text = getString(R.string.description_edit_save)
                addContributionButton.icon = null
            }
        } else if (action == TRANSLATE_DESCRIPTION || action == TRANSLATE_CAPTION) {
            addContributionButton.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_translation_button else R.string.suggested_edits_edit_translation_button)
        } else if (addContributionButton.tag == "portrait") {
            if (action == ADD_CAPTION) {
                addContributionButton.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_caption_button else R.string.suggested_edits_edit_caption_button)
            } else {
                addContributionButton.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_description_button else R.string.suggested_edits_edit_description_button)
            }
        }
    }

    override fun onDestroyView() {
        funnel?.stop()
        disposables.clear()
        cardsViewPager.unregisterOnPageChangeCallback(viewPagerListener)
        cardsViewPager.adapter = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        SuggestedEditsFunnel.get().pause()
    }

    override fun onResume() {
        super.onResume()
        SuggestedEditsFunnel.get().resume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT && resultCode == RESULT_OK) {
            logSuccess()
            topChild()?.showAddedContributionView(data?.getStringExtra(EXTRA_SOURCE_ADDED_CONTRIBUTION))
            FeedbackUtil.showMessage(this,
                    when (action) {
                        ADD_CAPTION -> getString(R.string.description_edit_success_saved_image_caption_snackbar)
                        TRANSLATE_CAPTION -> getString(R.string.description_edit_success_saved_image_caption_in_lang_snackbar, app.language().getAppLanguageLocalizedName(topChild()!!.targetSummaryForEdit!!.lang))
                        TRANSLATE_DESCRIPTION -> getString(R.string.description_edit_success_saved_in_lang_snackbar, app.language().getAppLanguageLocalizedName(topChild()!!.targetSummaryForEdit!!.lang))
                        else -> getString(R.string.description_edit_success_saved_snackbar)
                    }
            )
            nextPage(null)
        }
    }

    private fun previousPage() {
        viewPagerListener.setNextPageSelectedAutomatic()
        if (cardsViewPager.currentItem > 0) {
            cardsViewPager.setCurrentItem(cardsViewPager.currentItem - 1, true)
        }
        updateActionButton()
    }

    override fun nextPage(sourceFragment: Fragment?) {
        if (sourceFragment == topBaseChild() || sourceFragment == null) {
            viewPagerListener.setNextPageSelectedAutomatic()
            cardsViewPager.setCurrentItem(cardsViewPager.currentItem + 1, true)
            updateActionButton()
        }
    }

    override fun logSuccess() {
        funnel?.editSuccess()
    }

    fun onSelectPage() {
        if (action == ADD_IMAGE_TAGS && topBaseChild() != null) {
            topBaseChild()!!.publish()
        } else if (topTitle != null) {
            startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), topTitle!!, null, topChild()!!.sourceSummaryForEdit, topChild()!!.targetSummaryForEdit,
                    action, InvokeSource.SUGGESTED_EDITS), ACTIVITY_REQUEST_DESCRIPTION_EDIT)
        }
    }

    private fun requestLanguagesAndBuildSpinner() {
        disposables.add(ServiceFactory.get(app.wikiSite).siteMatrix
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { siteMatrix = it; }
                .doAfterTerminate { initLanguageSpinners() }
                .subscribe({
                    app.language().appLanguageCodes.forEach {
                        languageList.add(getLanguageLocalName(it))
                    }
                }, { L.e(it) }))
    }

    private fun getLanguageLocalName(code: String): String {
        if (siteMatrix == null) {
            return app.language().getAppLanguageLocalizedName(code)!!
        }
        var name: String? = null
        SiteMatrix.getSites(siteMatrix!!).forEach {
            if (code == it.code()) {
                name = it.name()
                return@forEach
            }
        }
        if (name.isNullOrEmpty()) {
            name = app.language().getAppLanguageLocalizedName(code)
        }
        return name ?: code
    }

    private fun resetViewPagerItemAdapter() {
        if (!resettingViewPager) {
            resettingViewPager = true
            val postDelay: Long = 250
            cardsViewPager.postDelayed({
                if (isAdded) {
                    cardsViewPager.adapter = ViewPagerAdapter(this)
                    resettingViewPager = false
                }
            }, postDelay)
        }
    }

    private fun setInitialUiState() {
        wikiLanguageDropdownContainer.visibility = if (app.language().appLanguageCodes.size > 1
                && (action == TRANSLATE_DESCRIPTION || action == TRANSLATE_CAPTION)) VISIBLE else GONE
    }

    private fun swapLanguageSpinnerSelection(isFromLang: Boolean) {
        if (!swappingLanguageSpinners) {
            swappingLanguageSpinners = true
            val preLangPosition = app.language().appLanguageCodes.indexOf(if (isFromLang) langFromCode else langToCode)
            if (isFromLang) {
                wikiToLanguageSpinner.setSelection(preLangPosition)
            } else {
                wikiFromLanguageSpinner.setSelection(preLangPosition)
            }
            swappingLanguageSpinners = false
        }
    }

    private fun initLanguageSpinners() {
        wikiFromLanguageSpinner.adapter = ArrayAdapter(requireContext(), R.layout.item_language_spinner, languageList)
        wikiToLanguageSpinner.adapter = ArrayAdapter(requireContext(), R.layout.item_language_spinner, languageList)
        wikiToLanguageSpinner.setSelection(app.language().appLanguageCodes.indexOf(langToCode))
    }

    private inner class OnFromSpinnerItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (langToCode == app.language().appLanguageCodes[position]) {
                swapLanguageSpinnerSelection(true)
            }

            if (!swappingLanguageSpinners && langFromCode != app.language().appLanguageCodes[position]) {
                langFromCode = app.language().appLanguageCodes[position]
                resetViewPagerItemAdapter()
                updateBackButton(0)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
        }
    }

    private inner class OnToSpinnerItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (langFromCode == app.language().appLanguageCodes[position]) {
                swapLanguageSpinnerSelection(false)
            }

            if (!swappingLanguageSpinners && langToCode != app.language().appLanguageCodes[position]) {
                langToCode = app.language().appLanguageCodes[position]
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
            return when (action) {
                ADD_IMAGE_TAGS -> {
                    SuggestedEditsImageTagsFragment.newInstance()
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
            updateBackButton(position)
            updateActionButton()
            SuggestedEditsFunnel.get().impression(action)

            nextPageSelectedAutomatic = false
            prevPosition = position

            val storedOffScreenPagesCount = cardsViewPager.offscreenPageLimit * 2 + 1
            if (position >= storedOffScreenPagesCount) {
                (cardsViewPager.adapter as ViewPagerAdapter).removeFragmentAt(position - storedOffScreenPagesCount)
            }
        }
    }

    companion object {
        fun newInstance(action: DescriptionEditActivity.Action, invokeSource: InvokeSource): SuggestedEditsCardsFragment {
            val addTitleDescriptionsFragment = SuggestedEditsCardsFragment()
            val args = Bundle()
            args.putSerializable(INTENT_EXTRA_ACTION, action)
            args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            addTitleDescriptionsFragment.arguments = args
            return addTitleDescriptionsFragment
        }
    }
}
