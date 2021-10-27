package org.wikipedia.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.AppLanguageSearchingFunnel
import org.wikipedia.databinding.ActivityLanguagesListBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.log.L
import java.util.*

class LanguagesListActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguagesListBinding
    private lateinit var searchActionModeCallback: LanguageSearchCallback
    private lateinit var searchingFunnel: AppLanguageSearchingFunnel

    private var app = WikipediaApp.getInstance()
    private var currentSearchQuery: String? = null
    private var actionMode: ActionMode? = null
    private var interactionsCount = 0
    private var isLanguageSearched = false
    private val disposables = CompositeDisposable()
    private var siteInfoList: List<SiteMatrix.SiteInfo>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguagesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.languagesListEmptyView.setEmptyText(R.string.langlinks_no_match)
        binding.languagesListEmptyView.visibility = View.GONE
        binding.languagesListRecycler.adapter = LanguagesListAdapter(app.language().appMruLanguageCodes, app.language().remainingAvailableLanguageCodes)
        binding.languagesListRecycler.layoutManager = LinearLayoutManager(this)
        binding.languagesListLoadProgress.visibility = View.VISIBLE
        searchActionModeCallback = LanguageSearchCallback()

        searchingFunnel = AppLanguageSearchingFunnel(intent.getStringExtra(WikipediaLanguagesFragment.SESSION_TOKEN).orEmpty())
        requestLanguages()
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_languages_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_language -> {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        DeviceUtil.hideSoftKeyboard(this)
        val returnIntent = Intent()
        returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched)
        setResult(RESULT_OK, returnIntent)
        searchingFunnel.logNoLanguageAdded(false, currentSearchQuery)
        super.onBackPressed()
    }

    private inner class LanguageSearchCallback : SearchActionModeCallback() {
        private val languageAdapter = binding.languagesListRecycler.adapter as LanguagesListAdapter

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // currentSearchQuery is cleared here, instead of onDestroyActionMode
            // in order to make the most recent search string available to analytics
            currentSearchQuery = ""
            isLanguageSearched = true
            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            currentSearchQuery = s.trim()
            languageAdapter.setFilterText(currentSearchQuery)
            if (binding.languagesListRecycler.adapter?.itemCount == 0) {
                binding.languagesListEmptyView.visibility = View.VISIBLE
            } else {
                binding.languagesListEmptyView.visibility = View.GONE
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            binding.languagesListEmptyView.visibility = View.GONE
            languageAdapter.reset()
            actionMode = null
        }

        override fun getSearchHintString(): String {
            return resources.getString(R.string.search_hint_search_languages)
        }

        override fun getParentContext(): Context {
            return this@LanguagesListActivity
        }
    }

    private inner class LanguagesListAdapter(languageCodes: List<String>, private val suggestedLanguageCodes: List<String>) : RecyclerView.Adapter<DefaultViewHolder>() {
        private val originalLanguageCodes = languageCodes.toMutableList()
        private val languageCodes = mutableListOf<String>()
        private var isSearching = false

        // To remove the already selected languages and suggested languages from all languages list
        private val nonDuplicateLanguageCodesList
            get() = originalLanguageCodes.toMutableList().apply {
                    removeAll(app.language().appLanguageCodes)
                    removeAll(suggestedLanguageCodes)
                }

        init {
            reset()
        }

        override fun getItemViewType(position: Int): Int {
            return if (shouldShowSectionHeader(position)) Companion.VIEW_TYPE_HEADER else Companion.VIEW_TYPE_ITEM
        }

        override fun getItemCount(): Int {
            return languageCodes.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == Companion.VIEW_TYPE_HEADER) {
                val view = inflater.inflate(R.layout.view_section_header, parent, false)
                DefaultViewHolder(languageCodes, view)
            } else {
                val view = inflater.inflate(R.layout.item_language_list_entry, parent, false)
                LanguagesListItemHolder(languageCodes, view)
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder, pos: Int) {
            holder.bindItem(pos)
            (holder as? LanguagesListItemHolder)?.itemView?.setOnClickListener {
                val lang = languageCodes[pos]
                if (lang != app.appOrSystemLanguageCode) {
                    app.language().addAppLanguageCode(lang)
                }
                interactionsCount++
                searchingFunnel.logLanguageAdded(true, lang, currentSearchQuery)
                DeviceUtil.hideSoftKeyboard(this@LanguagesListActivity)
                val returnIntent = Intent()
                returnIntent.putExtra(WikipediaLanguagesFragment.ADD_LANGUAGE_INTERACTIONS, interactionsCount)
                returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched)
                setResult(RESULT_OK, returnIntent)
                // Add the language to notifications preferences in order to receive notifications from that language wiki
                val notificationFilters = Prefs.notificationFilterCodes.orEmpty().toMutableSet()
                notificationFilters.add(lang)
                Prefs.notificationFilterCodes = notificationFilters.toSet()
                finish()
            }
        }

        fun shouldShowSectionHeader(position: Int): Boolean {
            return !isSearching && (position == 0 || (suggestedLanguageCodes.isNotEmpty() &&
                            position == suggestedLanguageCodes.size + 1))
        }

        fun setFilterText(filterText: String?) {
            isSearching = true
            languageCodes.clear()
            val filter = StringUtils.stripAccents(filterText).lowercase(Locale.getDefault())
            for (code in originalLanguageCodes) {
                val localizedName = StringUtils.stripAccents(app.language().getAppLanguageLocalizedName(code).orEmpty())
                val canonicalName = StringUtils.stripAccents(getCanonicalName(code).orEmpty())
                if (code.contains(filter) ||
                        localizedName.lowercase(Locale.getDefault()).contains(filter) ||
                        canonicalName.lowercase(Locale.getDefault()).contains(filter)) {
                    languageCodes.add(code)
                }
            }
            notifyDataSetChanged()
        }

        fun reset() {
            isSearching = false
            languageCodes.clear()
            if (suggestedLanguageCodes.isNotEmpty()) {
                languageCodes.add(getString(R.string.languages_list_suggested_text))
                languageCodes.addAll(suggestedLanguageCodes)
            }
            languageCodes.add(getString(R.string.languages_list_all_text))
            languageCodes.addAll(nonDuplicateLanguageCodesList)
            // should not be able to be searched while the languages are selected
            originalLanguageCodes.removeAll(app.language().appLanguageCodes)
            notifyDataSetChanged()
        }
    }

    private fun getCanonicalName(code: String): String? {
        var canonicalName = siteInfoList?.find { it.code == code }?.localname
        if (canonicalName.isNullOrEmpty()) {
            canonicalName = app.language().getAppLanguageCanonicalName(code)
        }
        return canonicalName
    }

    // TODO: optimize and reuse the header view holder?
    private open inner class DefaultViewHolder constructor(private val languageCodes: List<String>, itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindItem(position: Int) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = languageCodes[position]
        }
    }

    private inner class LanguagesListItemHolder constructor(private val languageCodes: List<String>, itemView: View) : DefaultViewHolder(languageCodes, itemView) {
        private val localizedNameTextView = itemView.findViewById<TextView>(R.id.localized_language_name)
        private val canonicalNameTextView = itemView.findViewById<TextView>(R.id.language_subtitle)

        override fun bindItem(position: Int) {
            val languageCode = languageCodes[position]
            localizedNameTextView.text = app.language().getAppLanguageLocalizedName(languageCode).orEmpty().capitalize(Locale.getDefault())
            val canonicalName = getCanonicalName(languageCode)
            if (binding.languagesListLoadProgress.visibility != View.VISIBLE) {
                canonicalNameTextView.text = if (canonicalName.isNullOrEmpty()) app.language().getAppLanguageCanonicalName(languageCode) else canonicalName
            }
        }
    }

    private fun requestLanguages() {
        disposables.add(ServiceFactory.get(app.wikiSite).siteMatrix
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { siteMatrix -> SiteMatrix.getSites(siteMatrix) }
                .doAfterTerminate {
                    binding.languagesListLoadProgress.visibility = View.INVISIBLE
                    binding.languagesListRecycler.adapter?.notifyDataSetChanged()
                }
                .subscribe({ sites -> siteInfoList = sites }) { t -> L.e(t) })
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        const val LANGUAGE_SEARCHED = "language_searched"
    }
}
