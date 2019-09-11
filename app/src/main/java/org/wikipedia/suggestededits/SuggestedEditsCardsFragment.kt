package org.wikipedia.suggestededits

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_suggested_edits_cards.*
import org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.InvokeSource.*
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.RandomizerFunnel
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.suggestededits.SuggestedEditsCardsActivity.Companion.EXTRA_SOURCE
import org.wikipedia.suggestededits.SuggestedEditsCardsActivity.Companion.EXTRA_SOURCE_ADDED_CONTRIBUTION
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L

class SuggestedEditsCardsFragment : Fragment() {
    private val viewPagerListener = ViewPagerListener()
    private var funnel: RandomizerFunnel? = null
    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    private var siteMatrix: SiteMatrix? = null
    private var languageList: MutableList<String> = mutableListOf()
    private var swappingLanguageSpinners: Boolean = false
    private var resettingViewPager: Boolean = false
    var langFromCode: String = app.language().appLanguageCode
    var langToCode: String = if (app.language().appLanguageCodes.size == 1) "" else app.language().appLanguageCodes[1]
    var source: InvokeSource = SUGGESTED_EDITS_ADD_DESC

    private val topTitle: PageTitle?
        get() {
            val f = topChild
            return if (source == SUGGESTED_EDITS_ADD_DESC || source == SUGGESTED_EDITS_ADD_CAPTION) {
                f?.sourceSummary?.pageTitle?.description = f?.addedContribution
                f?.sourceSummary?.pageTitle
            } else {
                f?.targetSummary?.pageTitle?.description = f?.addedContribution
                f?.targetSummary?.pageTitle
            }
        }

    private val topChild: SuggestedEditsCardsItemFragment?
        get() {
            fragmentManager!!.fragments.forEach {
                if (it is SuggestedEditsCardsItemFragment && it.pagerPosition == cardsViewPager.currentItem) {
                    return it
                }
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        source = arguments?.getSerializable(EXTRA_SOURCE) as InvokeSource

        // Record the first impression, since the ViewPager doesn't send an event for the first topmost item.
        SuggestedEditsFunnel.get().impression(source)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_suggested_edits_cards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialUiState()
        cardsViewPager.offscreenPageLimit = 2
        cardsViewPager.addOnPageChangeListener(viewPagerListener)
        resetViewPagerItemAdapter()

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
            nextPage()
        }
        updateBackButton(0)
        addContributionButton.setOnClickListener { onSelectPage() }
        updateActionButton()
    }

    private fun updateBackButton(pagerPosition: Int) {
        backButton.isClickable = pagerPosition != 0
        backButton.alpha = if (pagerPosition == 0) 0.31f else 1f
    }

    private fun updateActionButton() {
        val isAddedContributionEmpty = topChild?.addedContribution.isNullOrEmpty()
        if (!isAddedContributionEmpty) topChild?.showAddedContributionView(topChild?.addedContribution)
        addContributionImage!!.setImageDrawable(requireContext().getDrawable(if (isAddedContributionEmpty) R.drawable.ic_add_gray_white_24dp else R.drawable.ic_mode_edit_white_24dp))
        if (source == SUGGESTED_EDITS_TRANSLATE_DESC || source == SUGGESTED_EDITS_TRANSLATE_CAPTION) {
            addContributionText?.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_translation_button else R.string.suggested_edits_edit_translation_button)
        } else if (addContributionText != null) {
            if (source == SUGGESTED_EDITS_ADD_CAPTION) {
                addContributionText?.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_caption_button else R.string.suggested_edits_edit_caption_button)
            } else {
                addContributionText?.text = getString(if (isAddedContributionEmpty) R.string.suggested_edits_add_description_button else R.string.suggested_edits_edit_description_button)
            }
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        cardsViewPager.removeOnPageChangeListener(viewPagerListener)
        if (funnel != null) {
            funnel!!.done()
            funnel = null
        }
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
            topChild?.showAddedContributionView(data?.getStringExtra(EXTRA_SOURCE_ADDED_CONTRIBUTION))
            FeedbackUtil.showMessage(this,
                    when (source) {
                        SUGGESTED_EDITS_ADD_CAPTION -> getString(R.string.description_edit_success_saved_image_caption_snackbar)
                        SUGGESTED_EDITS_TRANSLATE_CAPTION -> getString(R.string.description_edit_success_saved_image_caption_in_lang_snackbar, app.language().getAppLanguageLocalizedName(topChild!!.targetSummary!!.lang))
                        SUGGESTED_EDITS_TRANSLATE_DESC -> getString(R.string.description_edit_success_saved_in_lang_snackbar, app.language().getAppLanguageLocalizedName(topChild!!.targetSummary!!.lang))
                        else -> getString(R.string.description_edit_success_saved_snackbar)
                    }
            )
            nextPage()
        }
    }

    private fun previousPage() {
        viewPagerListener.setNextPageSelectedAutomatic()
        if (cardsViewPager.currentItem > 0) {
            cardsViewPager.setCurrentItem(cardsViewPager.currentItem - 1, true)
        }
        updateActionButton()
    }

    private fun nextPage() {
        viewPagerListener.setNextPageSelectedAutomatic()
        cardsViewPager.setCurrentItem(cardsViewPager.currentItem + 1, true)
        updateActionButton()
    }

    fun onSelectPage() {
        if (topTitle != null) {
            startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), topTitle!!, null, topChild!!.sourceSummary, topChild!!.targetSummary, source),
                    ACTIVITY_REQUEST_DESCRIPTION_EDIT)
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
                    cardsViewPager.adapter = ViewPagerAdapter(requireActivity() as AppCompatActivity)
                    resettingViewPager = false
                }
            }, postDelay)
        }
    }

    private fun setInitialUiState() {
        wikiLanguageDropdownContainer.visibility = if (app.language().appLanguageCodes.size > 1
                && (source == SUGGESTED_EDITS_TRANSLATE_DESC || source == SUGGESTED_EDITS_TRANSLATE_CAPTION)) VISIBLE else GONE
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

    private class ViewPagerAdapter internal constructor(activity: AppCompatActivity): FragmentStatePagerAdapter(activity.supportFragmentManager) {

        override fun getCount(): Int {
            return Integer.MAX_VALUE
        }

        override fun getItem(position: Int): Fragment {
            val f = SuggestedEditsCardsItemFragment.newInstance()
            f.pagerPosition = position
            return f
        }
    }

    private inner class ViewPagerListener : ViewPager.OnPageChangeListener {
        private var prevPosition: Int = 0
        private var nextPageSelectedAutomatic: Boolean = false

        internal fun setNextPageSelectedAutomatic() {
            nextPageSelectedAutomatic = true
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            updateBackButton(position)
            updateActionButton()
            if (!nextPageSelectedAutomatic && funnel != null) {
                if (position > prevPosition) {
                    funnel!!.swipedForward()
                } else if (position < prevPosition) {
                    funnel!!.swipedBack()
                }
            }

            SuggestedEditsFunnel.get().impression(source)

            nextPageSelectedAutomatic = false
            prevPosition = position
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    companion object {
        fun newInstance(source: InvokeSource): SuggestedEditsCardsFragment {
            val addTitleDescriptionsFragment = SuggestedEditsCardsFragment()
            val args = Bundle()
            args.putSerializable(EXTRA_SOURCE, source)
            addTitleDescriptionsFragment.arguments = args
            return addTitleDescriptionsFragment
        }
    }
}
