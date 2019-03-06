package org.wikipedia.editactionfeed

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_title_descriptions.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.RandomizerFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TITLE_DESC_SOURCE
import org.wikipedia.descriptions.DescriptionEditActivity.EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE
import org.wikipedia.descriptions.DescriptionEditHelpActivity
import org.wikipedia.editactionfeed.AddTitleDescriptionsActivity.Companion.EXTRA_SOURCE
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.AnimationUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DialogTitleWithImage

class AddTitleDescriptionsFragment : Fragment() {
    private val viewPagerListener = ViewPagerListener()
    private var funnel: RandomizerFunnel? = null
    private val disposables = CompositeDisposable()
    private val app = WikipediaApp.getInstance()
    private var siteMatrix: SiteMatrix? = null
    private var languageList: MutableList<String> = mutableListOf()
    private var languageToList: MutableList<String> = mutableListOf()
    private var languageCodesToList: MutableList<String> = arrayListOf()
    var langFromCode: String = app.language().appLanguageCode
    var langToCode: String = if (app.language().appLanguageCodes.size == 1) "" else app.language().appLanguageCodes[1]
    var source: Int = EDIT_TASKS_TITLE_DESC_SOURCE
    var sourceDescription: CharSequence = ""

    private val topTitle: PageTitle?
        get() {
            val f = topChild
            return titleFromPageName(f?.title)
        }

    private val topChild: AddTitleDescriptionsItemFragment?
        get() {
            val fm = fragmentManager
            for (f in fm!!.fragments) {
                if (f is AddTitleDescriptionsItemFragment && f.pagerPosition == addTitleDescriptionsItemPager.currentItem) {
                    return f
                }
            }
            return null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // TODO: add funnel?
        source = arguments?.getInt(EXTRA_SOURCE, EDIT_TASKS_TITLE_DESC_SOURCE)!!
        return inflater.inflate(R.layout.fragment_add_title_descriptions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialUiState()
        wikiFromLanguageSpinner.onItemSelectedListener = OnFromSpinnerItemSelectedListener()
        wikiToLanguageSpinner.onItemSelectedListener = OnToSpinnerItemSelectedListener()

        addTitleDescriptionsItemPager.offscreenPageLimit = 2
        addTitleDescriptionsItemPager.setPageTransformer(true, AnimationUtil.PagerTransformer())
        addTitleDescriptionsItemPager.addOnPageChangeListener(viewPagerListener)

        resetTitleDescriptionItemAdapter()

        if (languageList.isEmpty()) {
            // Fragment is created for the first time.
            requestLanguagesAndBuildSpinner()
        } else {
            // Fragment already exists, so just update the UI.
            updateFromLanguageSpinner()
        }

        skipButton.setOnClickListener { nextPage() }

        addDescriptionButton.setOnClickListener {
            if (topTitle != null) {
                startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), topTitle!!, null, true, source, sourceDescription),
                        Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT)
            }
        }

        arrows.setOnClickListener {
            val pos = languageList.indexOf(languageToList[wikiToLanguageSpinner.selectedItemPosition])
            val prevFromLang = languageList[wikiFromLanguageSpinner.selectedItemPosition]
            wikiFromLanguageSpinner.setSelection(pos)
            val postDelay: Long = 100
            wikiToLanguageSpinner.postDelayed({
                if (isAdded) {
                    wikiToLanguageSpinner.setSelection(languageToList.indexOf(prevFromLang))
                }
            }, postDelay)
        }

        showOnboarding()
    }

    override fun onDestroyView() {
        disposables.clear()
        addTitleDescriptionsItemPager.removeOnPageChangeListener(viewPagerListener)
        if (funnel != null) {
            funnel!!.done()
            funnel = null
        }
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT && resultCode == RESULT_OK) {
            nextPage()
        }
    }

    private fun nextPage() {
        viewPagerListener.setNextPageSelectedAutomatic()
        addTitleDescriptionsItemPager.setCurrentItem(addTitleDescriptionsItemPager.currentItem + 1, true)
    }

    private fun titleFromPageName(pageName: String?): PageTitle {
        return PageTitle(pageName, WikiSite.forLanguageCode(if (source == EDIT_TASKS_TITLE_DESC_SOURCE) langFromCode else langToCode))
    }

    fun onSelectPage(pageName: String) {
        val title = titleFromPageName(pageName)
        startActivity(PageActivity.newIntentForNewTab(requireActivity(),
                HistoryEntry(title, HistoryEntry.SOURCE_RANDOM), title))
    }

    private fun showOnboarding() {
        if (Prefs.showEditActionAddTitleDescriptionsOnboarding() && source == EDIT_TASKS_TITLE_DESC_SOURCE) {
            AlertDialog.Builder(requireActivity())
                    .setCustomTitle(DialogTitleWithImage(requireActivity(), R.string.add_title_descriptions_dialog_title, R.drawable.ic_dialog_image_title_descriptions, false))
                    .setMessage(R.string.add_title_descriptions_dialog_message)
                    .setPositiveButton(R.string.title_descriptions_onboarding_got_it, null)
                    .setNegativeButton(R.string.editactionfeed_add_title_dialog_learn_more) { _, _ ->
                        startActivity(DescriptionEditHelpActivity.newIntent(requireContext()))
                    }
                    .show()
            Prefs.setShowEditActionAddTitleDescriptionsOnboarding(false)
        }

        if (Prefs.showEditActionTranslateDescriptionsOnboarding() && source == EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE) {
            AlertDialog.Builder(requireActivity())
                    .setCustomTitle(DialogTitleWithImage(requireActivity(), R.string.add_translate_descriptions_dialog_title, R.drawable.ic_dialog_image_title_descriptions, false))
                    .setMessage(R.string.add_translate_descriptions_dialog_message)
                    .setPositiveButton(R.string.translate_descriptions_onboarding_got_it, null)
                    .setNegativeButton(R.string.editactionfeed_translate_title_dialog_learn_more) { _, _ ->
                        startActivity(DescriptionEditHelpActivity.newIntent(requireContext()))
                    }
                    .show()
            Prefs.setShowEditActionTranslateDescriptionsOnboarding(false)
        }

    }

    private fun requestLanguagesAndBuildSpinner() {
        disposables.add(ServiceFactory.get(app.wikiSite).siteMatrix
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { siteMatrix = it; }
                .doFinally { updateFromLanguageSpinner() }
                .subscribe({
                    for (code in app.language().appLanguageCodes) {
                        // TODO: confirm: do we have to show the "WIKIPEDIA" text after the language name?
                        languageList.add(getLanguageLocalName(code))
                    }
                }, { L.e(it) }))
    }

    private fun getLanguageLocalName(code: String): String {
        if (siteMatrix == null) {
            return app.language().getAppLanguageLocalizedName(code)!!
        }
        var name: String? = null
        for (info in SiteMatrix.getSites(siteMatrix!!)) {
            if (code == info.code()) {
                name = info.name()
                break
            }
        }
        if (TextUtils.isEmpty(name)) {
            name = app.language().getAppLanguageLocalizedName(code)
        }
        return name ?: code
    }

    private fun resetTitleDescriptionItemAdapter() {
        val postDelay: Long = 250
        wikiToLanguageSpinner.postDelayed({
            if (isAdded) {
                addTitleDescriptionsItemPager.adapter = ViewPagerAdapter(requireActivity() as AppCompatActivity)
            }
        }, postDelay)
    }

    private fun setInitialUiState() {
        wikiLanguageDropdownContainer.visibility = if (app.language().appLanguageCodes.size > 1) VISIBLE else GONE

        if (source == EDIT_TASKS_TRANSLATE_TITLE_DESC_SOURCE) {
            fromLabel.visibility = GONE
            arrows.visibility = VISIBLE
            wikiToLanguageSpinner.visibility = VISIBLE
        } else {
            fromLabel.visibility = VISIBLE
            arrows.visibility = GONE
            wikiToLanguageSpinner.visibility = GONE
        }
    }

    private fun updateFromLanguageSpinner() {
        wikiFromLanguageSpinner.adapter = ArrayAdapter<String>(requireContext(), R.layout.item_language_spinner, languageList)
    }

    private fun updateToLanguageSpinner(fromLanguageSpinnerPosition: Int) {
        languageCodesToList.clear()
        languageCodesToList.addAll(app.language().appLanguageCodes)
        languageCodesToList.removeAt(fromLanguageSpinnerPosition)
        languageToList.clear()
        for (language in languageCodesToList) {
            languageToList.add(getLanguageLocalName(language))
        }

        val toAdapter = ArrayAdapter<String>(requireContext(), R.layout.item_language_spinner, languageToList)
        wikiToLanguageSpinner.adapter = toAdapter

        val pos = languageCodesToList.indexOf(langToCode)
        if (pos < 0) {
            langToCode = languageCodesToList[0]
        } else {
            wikiToLanguageSpinner.setSelection(pos)
        }
    }

    private inner class OnFromSpinnerItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (langFromCode != app.language().appLanguageCodes[position]) {
                langFromCode = app.language().appLanguageCodes[position]
                resetTitleDescriptionItemAdapter()
            }
            updateToLanguageSpinner(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
        }
    }

    private inner class OnToSpinnerItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (langToCode != languageCodesToList[position]) {
                langToCode = languageCodesToList[position]
                resetTitleDescriptionItemAdapter()
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
            val f = AddTitleDescriptionsItemFragment.newInstance()
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
            if (!nextPageSelectedAutomatic && funnel != null) {
                if (position > prevPosition) {
                    funnel!!.swipedForward()
                } else if (position < prevPosition) {
                    funnel!!.swipedBack()
                }
            }
            nextPageSelectedAutomatic = false
            prevPosition = position
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    companion object {
        fun newInstance(source: Int): AddTitleDescriptionsFragment {
            val addTitleDescriptionsFragment = AddTitleDescriptionsFragment()
            val args = Bundle()
            args.putInt(EXTRA_SOURCE, source)
            addTitleDescriptionsFragment.arguments = args
            return addTitleDescriptionsFragment
        }
    }
}
