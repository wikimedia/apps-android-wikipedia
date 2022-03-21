package org.wikipedia.language

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityLanglinksBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewAnimations
import java.util.*

class LangLinksActivity : BaseActivity() {
    private lateinit var binding: ActivityLanglinksBinding

    private var app = WikipediaApp.getInstance()

    private var currentSearchQuery: String? = null
    private var actionMode: ActionMode? = null
    private val viewModel: LangLinksViewModel by viewModels { LangLinksViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanglinksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.langlinksRecycler.itemAnimator = null
        binding.langlinkEmptyView.visibility = View.GONE
        binding.langlinksLoadProgress.visibility = View.VISIBLE

        binding.langlinksError.backClickListener = View.OnClickListener { onBackPressed() }
        binding.langlinksError.retryClickListener = View.OnClickListener {
            ViewAnimations.crossFade(binding.langlinksError, binding.langlinksLoadProgress)
            viewModel.fetchLangLinks()
        }

        viewModel.languageEntries.observe(this) {
            if (it is Resource.Success) {
                displayLangLinks(it.data)
            } else if (it is Resource.Error) {
                binding.langlinksError.setError(it.throwable)
                ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinksError)
            }
        }

        viewModel.siteListData.observe(this) {
            if (it is Resource.Success) {
                binding.langlinksRecycler.adapter?.notifyItemRangeChanged(0,
                        binding.langlinksRecycler.adapter?.itemCount!!)
            }
        }

        viewModel.languageEntryVariantUpdate.observe(this) {
            if (it is Resource.Success) {
                binding.langlinksRecycler.adapter?.notifyItemRangeChanged(0,
                        binding.langlinksRecycler.adapter?.itemCount!!)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_languages_list, menu)
        val searchIcon = menu.getItem(0)
        searchIcon.isVisible = viewModel.languageEntries.value is Resource.Success &&
                (viewModel.languageEntries.value as Resource.Success).data.isNotEmpty()
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

    private fun displayLangLinks(languageEntries: List<PageTitle>) {
        if (languageEntries.isEmpty()) {
            // TODO: Question: should we use the same empty view for the default empty view and search result empty view?
            binding.langlinkEmptyView.setEmptyText(R.string.langlinks_empty)
            ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinkEmptyView)
        } else {
            binding.langlinkEmptyView.setEmptyText(R.string.langlinks_no_match)
            binding.langlinksRecycler.adapter = LangLinksAdapter(languageEntries,
                    languageEntries.filter {
                        it.wikiSite.languageCode == AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE &&
                                app.language().appLanguageCodes.contains(AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE) ||
                                app.language().appLanguageCodes.contains(it.wikiSite.languageCode)
                    })
            binding.langlinksRecycler.layoutManager = LinearLayoutManager(this)
            ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinksRecycler)
        }
        invalidateOptionsMenu()
    }

    private inner class LangLinksAdapter(languageEntries: List<PageTitle>, private val appLanguageEntries: List<PageTitle>) : RecyclerView.Adapter<DefaultViewHolder>() {
        private val originalLanguageEntries = languageEntries.toMutableList()
        private val languageEntries = mutableListOf<PageTitle>()
        private val variantTitlesToUpdate = originalLanguageEntries.filter { !WikipediaApp.getInstance().language().getDefaultLanguageCode(it.wikiSite.languageCode).isNullOrEmpty() }.toMutableList()

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
            return languageEntries.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            return if (viewType == Companion.VIEW_TYPE_HEADER) {
                val view = inflater.inflate(R.layout.view_section_header, parent, false)
                DefaultViewHolder(view)
            } else {
                val view = inflater.inflate(R.layout.item_langlinks_list_entry, parent, false)
                LangLinksItemViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder, pos: Int) {
            if (variantTitlesToUpdate.contains(languageEntries[pos])) {
                viewModel.fetchLangVariantLink(languageEntries[pos])
                variantTitlesToUpdate.remove(languageEntries[pos])
            }
            holder.bindItem(languageEntries[pos])
        }

        fun shouldShowSectionHeader(position: Int): Boolean {
            return !isSearching && (position == 0 || (appLanguageEntries.isNotEmpty() && position == appLanguageEntries.size + 1))
        }

        fun setFilterText(filterText: String) {
            isSearching = true
            languageEntries.clear()
            val filter = filterText.lowercase(Locale.getDefault())
            for (entry in originalLanguageEntries) {
                val languageCode = entry.wikiSite.languageCode
                val canonicalName = app.language().getAppLanguageCanonicalName(languageCode).orEmpty()
                val localizedName = app.language().getAppLanguageLocalizedName(languageCode).orEmpty()
                if (canonicalName.lowercase(Locale.getDefault()).contains(filter) ||
                        localizedName.lowercase(Locale.getDefault()).contains(filter)) {
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

    private open inner class DefaultViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindItem(pageTitle: PageTitle) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = StringUtil.fromHtml(pageTitle.displayText)
        }
    }

    private inner class LangLinksItemViewHolder constructor(itemView: View) : DefaultViewHolder(itemView), View.OnClickListener {
        private val localizedLanguageNameTextView = itemView.findViewById<TextView>(R.id.localized_language_name)
        private val nonLocalizedLanguageNameTextView = itemView.findViewById<TextView>(R.id.non_localized_language_name)
        private val articleTitleTextView = itemView.findViewById<TextView>(R.id.language_subtitle)
        private lateinit var pageTitle: PageTitle

        override fun bindItem(pageTitle: PageTitle) {
            this.pageTitle = pageTitle
            val languageCode = pageTitle.wikiSite.languageCode
            val localizedLanguageName = app.language().getAppLanguageLocalizedName(languageCode)
            localizedLanguageNameTextView.text = localizedLanguageName?.capitalize(Locale.getDefault())
                    ?: languageCode
            articleTitleTextView.text = pageTitle.displayText
            val canonicalName = viewModel.getCanonicalName(languageCode)
            if (canonicalName.isNullOrEmpty() || languageCode == app.language().systemLanguageCode) {
                nonLocalizedLanguageNameTextView.visibility = View.GONE
            } else {
                // TODO: Fix an issue when app language is zh-hant, the subtitle in zh-hans will display in English
                nonLocalizedLanguageNameTextView.text = canonicalName
                nonLocalizedLanguageNameTextView.visibility = View.VISIBLE
            }
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            app.language().addMruLanguageCode(pageTitle.wikiSite.languageCode)
            val historyEntry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_LANGUAGE_LINK)
            val intent = PageActivity.newIntentForCurrentTab(this@LangLinksActivity, historyEntry, pageTitle, false)
            setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent)
            DeviceUtil.hideSoftKeyboard(this@LangLinksActivity)
            finish()
        }
    }

    companion object {
        const val ACTIVITY_RESULT_LANGLINK_SELECT = 1
        const val ACTION_LANGLINKS_FOR_TITLE = "org.wikipedia.langlinks_for_title"
        const val EXTRA_PAGETITLE = "org.wikipedia.pagetitle"

        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }
}
