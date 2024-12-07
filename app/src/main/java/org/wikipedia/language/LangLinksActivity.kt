package org.wikipedia.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
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

class LangLinksActivity : BaseActivity() {
    private lateinit var binding: ActivityLanglinksBinding

    private var app = WikipediaApp.instance

    private var currentSearchQuery: String? = null
    private var actionMode: ActionMode? = null
    private val viewModel: LangLinksViewModel by viewModels()

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
                        (it.wikiSite.languageCode == AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE &&
                                app.languageState.appLanguageCodes.contains(AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE)) ||
                                (it.wikiSite.languageCode == AppLanguageLookUpTable.BELARUSIAN_TARASK_LANGUAGE_CODE &&
                                app.languageState.appLanguageCodes.contains(AppLanguageLookUpTable.BELARUSIAN_LEGACY_LANGUAGE_CODE)) ||
                                app.languageState.appLanguageCodes.contains(it.wikiSite.languageCode)
                    })
            binding.langlinksRecycler.layoutManager = LinearLayoutManager(this)
            ViewAnimations.crossFade(binding.langlinksLoadProgress, binding.langlinksRecycler)
        }
        invalidateOptionsMenu()
    }

    private inner class LangLinksAdapter(languageEntries: List<PageTitle>, private val appLanguageEntries: List<PageTitle>) : RecyclerView.Adapter<DefaultViewHolder>() {
        private val originalLanguageEntries = languageEntries.toMutableList()
        private val languageEntries = mutableListOf<PageTitle>()
        private val variantLangsToUpdate = originalLanguageEntries.mapNotNull { WikipediaApp.instance.languageState.getDefaultLanguageCode(it.wikiSite.languageCode) }.toMutableSet()

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
            val langCode = WikipediaApp.instance.languageState.getDefaultLanguageCode(languageEntries[pos].wikiSite.languageCode)
            if (langCode != null && variantLangsToUpdate.contains(langCode)) {
                variantLangsToUpdate.remove(langCode)
                viewModel.fetchLangVariantLinks(langCode, languageEntries[pos].prefixedText, originalLanguageEntries)
            }
            holder.bindItem(languageEntries[pos])
        }

        fun shouldShowSectionHeader(position: Int): Boolean {
            return !isSearching && (position == 0 || (appLanguageEntries.isNotEmpty() && position == appLanguageEntries.size + 1))
        }

        fun setFilterText(filterText: String) {
            isSearching = true
            languageEntries.clear()
            for (entry in originalLanguageEntries) {
                val languageCode = entry.wikiSite.languageCode
                val canonicalName = app.languageState.getAppLanguageCanonicalName(languageCode).orEmpty()
                val localizedName = app.languageState.getAppLanguageLocalizedName(languageCode).orEmpty()
                if (canonicalName.contains(filterText, true) || localizedName.contains(filterText, true)) {
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

    private open inner class DefaultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindItem(pageTitle: PageTitle) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = StringUtil.fromHtml(pageTitle.displayText)
        }
    }

    private inner class LangLinksItemViewHolder(itemView: View) : DefaultViewHolder(itemView), View.OnClickListener {
        private val localizedLanguageNameTextView = itemView.findViewById<TextView>(R.id.localized_language_name)
        private val nonLocalizedLanguageNameTextView = itemView.findViewById<TextView>(R.id.non_localized_language_name)
        private val articleTitleTextView = itemView.findViewById<TextView>(R.id.language_subtitle)
        private lateinit var pageTitle: PageTitle

        override fun bindItem(pageTitle: PageTitle) {
            this.pageTitle = pageTitle
            val languageCode = pageTitle.wikiSite.languageCode
            val localizedLanguageName = app.languageState.getAppLanguageLocalizedName(languageCode)
            localizedLanguageNameTextView.text = StringUtil.capitalize(localizedLanguageName) ?: languageCode
            articleTitleTextView.text = StringUtil.fromHtml(pageTitle.displayText)
            val canonicalName = viewModel.getCanonicalName(languageCode)
            if (canonicalName.isNullOrEmpty() || languageCode == app.languageState.systemLanguageCode) {
                nonLocalizedLanguageNameTextView.visibility = View.GONE
            } else {
                nonLocalizedLanguageNameTextView.text = canonicalName
                nonLocalizedLanguageNameTextView.visibility = View.VISIBLE
            }
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            app.languageState.addMruLanguageCode(pageTitle.wikiSite.languageCode)
            val intent = PageActivity.newIntentForCurrentTab(this@LangLinksActivity, HistoryEntry(pageTitle, HistoryEntry.SOURCE_LANGUAGE_LINK), pageTitle, false)
            setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent)
            DeviceUtil.hideSoftKeyboard(this@LangLinksActivity)
            finish()
        }
    }

    companion object {
        const val ACTIVITY_RESULT_LANGLINK_SELECT = 1
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        fun newIntent(context: Context, title: PageTitle): Intent {
            return Intent(context, LangLinksActivity::class.java)
                .putExtra(Constants.ARG_TITLE, title)
        }
    }
}
