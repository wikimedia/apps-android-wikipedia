package org.wikipedia.language

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityLanglinksBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewAnimations
import java.util.*

class LangLinksActivity : BaseActivity() {
    private lateinit var binding: ActivityLanglinksBinding
    private lateinit var title: PageTitle

    private var languageEntries = mutableListOf<PageTitle>()
    private var app = WikipediaApp.getInstance()
    private val disposables = CompositeDisposable()

    private var currentSearchQuery: String? = null
    private var actionMode: ActionMode? = null
    private var siteInfoList: List<SiteMatrix.SiteInfo>? = null

    private val entriesByAppLanguages: List<PageTitle>
        get() = languageEntries.filter {
            it.wikiSite.languageCode() == AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE &&
                    app.language().appLanguageCodes.contains(AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE) ||
                    app.language().appLanguageCodes.contains(it.wikiSite.languageCode())
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanglinksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = intent.getParcelableExtra(EXTRA_PAGETITLE)!!
        binding.langlinkEmptyView.visibility = View.GONE
        binding.langlinksLoadProgress.visibility = View.VISIBLE
        fetchLangLinks()

        binding.langlinksError.backClickListener = View.OnClickListener { onBackPressed() }
        binding.langlinksError.retryClickListener = View.OnClickListener {
            ViewAnimations.crossFade(binding.langlinksError, binding.langlinksLoadProgress)
            fetchLangLinks()
        }
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_languages_list, menu)
        val searchIcon = menu.getItem(0)
        searchIcon.isVisible = languageEntries.isNotEmpty()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_language -> {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(LanguageSearchCallback())
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        DeviceUtil.hideSoftKeyboard(this)
        super.onBackPressed()
    }

    private inner class LanguageSearchCallback : SearchActionModeCallback() {
        private val langLinksAdapter = binding.langlinksRecycler.adapter as LangLinksAdapter

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            currentSearchQuery = s.trim()
            langLinksAdapter.setFilterText(currentSearchQuery!!)
            if (binding.langlinksRecycler.adapter?.itemCount == 0) {
                binding.langlinkEmptyView.visibility = View.VISIBLE
            } else {
                binding.langlinkEmptyView.visibility = View.GONE
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            if (!currentSearchQuery.isNullOrEmpty()) {
                currentSearchQuery = ""
            }
            binding.langlinkEmptyView.visibility = View.GONE
            langLinksAdapter.reset()
            actionMode = null
        }

        override fun getSearchHintString(): String {
            return resources.getString(R.string.langlinks_filter_hint)
        }

        override fun getParentContext(): Context {
            return this@LangLinksActivity
        }
    }

    private fun displayLangLinks() {
        if (languageEntries.isEmpty()) {
            // TODO: Question: should we use the same empty view for the default empty view and search result empty view?
            binding.langlinkEmptyView.setEmptyText(R.string.langlinks_empty)
            ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinkEmptyView)
        } else {
            binding.langlinkEmptyView.setEmptyText(R.string.langlinks_no_match)
            binding.langlinksRecycler.adapter = LangLinksAdapter(languageEntries, entriesByAppLanguages)
            binding.langlinksRecycler.layoutManager = LinearLayoutManager(this)
            disposables.add(ServiceFactory.get(app.wikiSite).siteMatrix
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { siteMatrix -> SiteMatrix.getSites(siteMatrix) }
                    .doAfterTerminate {
                        binding.langlinksLoadProgress.visibility = View.INVISIBLE
                        binding.langlinksRecycler.adapter?.notifyDataSetChanged()
                    }
                    .subscribe({ sites -> siteInfoList = sites }) { t -> L.e(t) })
            ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinksRecycler)
        }
        invalidateOptionsMenu()
    }

    private fun fetchLangLinks() {
        if (languageEntries.isEmpty()) {
            disposables.add(ServiceFactory.get(title.wikiSite).getLangLinks(title.prefixedText)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        languageEntries = response.query()!!.langLinks()
                        updateLanguageEntriesSupported(languageEntries)
                        sortLanguageEntriesByMru(languageEntries)
                        displayLangLinks()
                    }) { t ->
                        ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinksError)
                        binding.langlinksError.setError(t)
                    })
        } else {
            displayLangLinks()
        }
    }

    private fun updateLanguageEntriesSupported(languageEntries: MutableList<PageTitle>) {
        val it = languageEntries.listIterator()
        while (it.hasNext()) {
            val link = it.next()
            val languageCode = link.wikiSite.languageCode()
            val languageVariants = app.language().getLanguageVariants(languageCode)
            if (AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE == languageCode) {
                // Replace legacy name of тарашкевіца language with the correct name.
                // TODO: Can probably be removed when T111853 is resolved.
                it.remove()
                it.add(PageTitle(link.text, WikiSite.forLanguageCode(AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE)))
            } else if (languageVariants != null) {
                // remove the language code and replace it with its variants
                it.remove()
                for (variant in languageVariants) {
                    it.add(PageTitle(if (title.isMainPage) SiteInfoClient.getMainPageForLang(variant) else link.prefixedText,
                            WikiSite.forLanguageCode(variant)))
                }
            }
        }
        addVariantEntriesIfNeeded(app.language(), title, languageEntries)
    }

    private fun sortLanguageEntriesByMru(entries: MutableList<PageTitle>) {
        var addIndex = 0
        for (language in app.language().mruLanguageCodes) {
            for (i in entries.indices) {
                if (entries[i].wikiSite.languageCode() == language) {
                    val entry = entries.removeAt(i)
                    entries.add(addIndex++, entry)
                    break
                }
            }
        }
    }

    private inner class LangLinksAdapter(languageEntries: List<PageTitle>, private val appLanguageEntries: List<PageTitle>) : RecyclerView.Adapter<DefaultViewHolder>() {
        private val originalLanguageEntries = languageEntries.toMutableList()
        private val languageEntries = mutableListOf<PageTitle>()
        private var isSearching = false

        // To remove the already selected languages and suggested languages from all languages list
        private val nonDuplicateEntries get() = originalLanguageEntries.toMutableList().apply { removeAll(appLanguageEntries) }

        init {
            reset()
        }

        override fun getItemViewType(position: Int): Int {
            return if (shouldShowSectionHeader(position)) Companion.VIEW_TYPE_HEADER else Companion.VIEW_TYPE_ITEM
        }

        override fun getItemCount(): Int {
            return this.languageEntries.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            return if (viewType == Companion.VIEW_TYPE_HEADER) {
                val view = inflater.inflate(R.layout.view_section_header, parent, false)
                DefaultViewHolder(languageEntries, view)
            } else {
                val view = inflater.inflate(R.layout.item_langlinks_list_entry, parent, false)
                LangLinksItemViewHolder(languageEntries, view)
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder, pos: Int) {
            holder.bindItem(pos)
        }

        fun shouldShowSectionHeader(position: Int): Boolean {
            return !isSearching && (position == 0 || (appLanguageEntries.isNotEmpty() && position == appLanguageEntries.size + 1))
        }

        fun setFilterText(filterText: String) {
            isSearching = true
            languageEntries.clear()
            val filter = filterText.toLowerCase(Locale.getDefault())
            for (entry in originalLanguageEntries) {
                val languageCode = entry.wikiSite.languageCode()
                val canonicalName = app.language().getAppLanguageCanonicalName(languageCode).orEmpty()
                val localizedName = app.language().getAppLanguageLocalizedName(languageCode).orEmpty()
                if (canonicalName.toLowerCase(Locale.getDefault()).contains(filter) ||
                        localizedName.toLowerCase(Locale.getDefault()).contains(filter)) {
                    languageEntries.add(entry)
                }
            }
            notifyDataSetChanged()
        }

        fun reset() {
            isSearching = false
            this.languageEntries.clear()
            if (appLanguageEntries.isNotEmpty()) {
                languageEntries.add(PageTitle(getString(R.string.langlinks_your_wikipedia_languages), app.wikiSite))
                languageEntries.addAll(appLanguageEntries)
            }
            val remainingEntries = nonDuplicateEntries
            if (remainingEntries.isNotEmpty()) {
                languageEntries.add(PageTitle(getString(R.string.languages_list_all_text), app.wikiSite))
                languageEntries.addAll(remainingEntries)
            }
            notifyDataSetChanged()
        }
    }

    private open inner class DefaultViewHolder constructor(private val languageEntries: List<PageTitle>, itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindItem(position: Int) {
            // TODO: Optimize this
            itemView.findViewById<TextView>(R.id.section_header_text).text = languageEntries[position].displayText
        }
    }

    private inner class LangLinksItemViewHolder constructor(private val languageEntries: List<PageTitle>, itemView: View) : DefaultViewHolder(languageEntries, itemView), View.OnClickListener {
        private val localizedLanguageNameTextView = itemView.findViewById<TextView>(R.id.localized_language_name)
        private val nonLocalizedLanguageNameTextView = itemView.findViewById<TextView>(R.id.non_localized_language_name)
        private val articleTitleTextView = itemView.findViewById<TextView>(R.id.language_subtitle)
        private var pos = 0

        override fun bindItem(position: Int) {
            pos = position
            val item = languageEntries[position]
            val languageCode = item.wikiSite.languageCode()
            val localizedLanguageName = app.language().getAppLanguageLocalizedName(languageCode)
            localizedLanguageNameTextView.text = localizedLanguageName?.capitalize(Locale.getDefault()) ?: languageCode
            articleTitleTextView.text = item.displayText
            if (binding.langlinksLoadProgress.visibility != View.VISIBLE) {
                val canonicalName = getCanonicalName(languageCode)
                if (canonicalName.isNullOrEmpty() || languageCode == app.language().systemLanguageCode) {
                    nonLocalizedLanguageNameTextView.visibility = View.GONE
                } else {
                    // TODO: Fix an issue when app language is zh-hant, the subtitle in zh-hans will display in English
                    nonLocalizedLanguageNameTextView.text = canonicalName
                    nonLocalizedLanguageNameTextView.visibility = View.VISIBLE
                }
            }
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val langLink = languageEntries[pos]
            app.language().addMruLanguageCode(langLink.wikiSite.languageCode())
            val historyEntry = HistoryEntry(langLink, HistoryEntry.SOURCE_LANGUAGE_LINK)
            val intent = PageActivity.newIntentForCurrentTab(this@LangLinksActivity, historyEntry, langLink, false)
            setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent)
            DeviceUtil.hideSoftKeyboard(this@LangLinksActivity)
            finish()
        }
    }

    private fun getCanonicalName(code: String): String? {
        var canonicalName = siteInfoList?.find { it.code() == code }?.localName()
        if (canonicalName.isNullOrEmpty()) {
            canonicalName = app.language().getAppLanguageCanonicalName(code)
        }
        return canonicalName
    }

    companion object {
        const val ACTIVITY_RESULT_LANGLINK_SELECT = 1
        const val ACTION_LANGLINKS_FOR_TITLE = "org.wikipedia.langlinks_for_title"
        const val EXTRA_PAGETITLE = "org.wikipedia.pagetitle"

        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        @JvmStatic
        fun addVariantEntriesIfNeeded(language: AppLanguageState, title: PageTitle, languageEntries: MutableList<PageTitle>) {
            val parentLanguageCode = language.getDefaultLanguageCode(title.wikiSite.languageCode())
            if (parentLanguageCode != null) {
                val languageVariants = language.getLanguageVariants(parentLanguageCode)
                if (languageVariants != null) {
                    for (languageCode in languageVariants) {
                        if (!title.wikiSite.languageCode().contains(languageCode)) {
                            val pageTitle = PageTitle(if (title.isMainPage) SiteInfoClient.getMainPageForLang(languageCode) else title.displayText, WikiSite.forLanguageCode(languageCode))
                            pageTitle.text = StringUtil.removeNamespace(title.prefixedText)
                            languageEntries.add(pageTitle)
                        }
                    }
                }
            }
        }
    }
}
