package org.wikipedia.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.AppLanguageSearchingFunnel
import org.wikipedia.databinding.ActivityLanguagesListBinding
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.Resource
import java.util.*

class LanguagesListActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguagesListBinding
    private lateinit var languageAdapter: LanguagesListAdapter
    private lateinit var searchActionModeCallback: LanguageSearchCallback
    private lateinit var searchingFunnel: AppLanguageSearchingFunnel

    private var app = WikipediaApp.instance
    private var currentSearchQuery: String? = null
    private var actionMode: ActionMode? = null
    private var interactionsCount = 0
    private var isLanguageSearched = false

    private val viewModel: LanguagesListViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguagesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.languagesListEmptyView.setEmptyText(R.string.langlinks_no_match)
        binding.languagesListEmptyView.visibility = View.GONE
        languageAdapter = LanguagesListAdapter()
        binding.languagesListRecycler.adapter = languageAdapter
        binding.languagesListRecycler.layoutManager = LinearLayoutManager(this)
        binding.languagesListLoadProgress.visibility = View.VISIBLE
        searchActionModeCallback = LanguageSearchCallback()

        searchingFunnel = AppLanguageSearchingFunnel(intent.getStringExtra(WikipediaLanguagesFragment.SESSION_TOKEN).orEmpty())

        viewModel.siteListData.observe(this, {
            if (it is Resource.Success) {
                binding.languagesListLoadProgress.visibility = View.INVISIBLE
                languageAdapter.notifyItemRangeChanged(0, languageAdapter.itemCount)
            }
        })
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
            languageAdapter.setFilterText(null)
            actionMode = null
        }

        override fun getSearchHintString(): String {
            return resources.getString(R.string.search_hint_search_languages)
        }

        override fun getParentContext(): Context {
            return this@LanguagesListActivity
        }
    }

    private inner class LanguagesListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), View.OnClickListener {
        var listItems = listOf<LanguagesListViewModel.LanguageListItem>()

        init {
            setFilterText(null)
        }

        override fun getItemViewType(position: Int): Int {
            return if (listItems[position].isHeader) Companion.VIEW_TYPE_HEADER else Companion.VIEW_TYPE_ITEM
        }

        override fun getItemCount(): Int {
            return listItems.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == Companion.VIEW_TYPE_HEADER) {
                HeaderViewHolder(inflater.inflate(R.layout.view_section_header, parent, false))
            } else {
                LanguagesListItemHolder(inflater.inflate(R.layout.item_language_list_entry, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is HeaderViewHolder) {
                holder.bindItem(listItems[pos])
            } else if (holder is LanguagesListItemHolder) {
                holder.bindItem(listItems[pos])
                holder.itemView.setOnClickListener(this)
            }
            holder.itemView.tag = pos
        }

        fun setFilterText(filterText: String?) {
            val newListItems = viewModel.getListBySearchTerm(this@LanguagesListActivity, filterText)
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return listItems.size
                }

                override fun getNewListSize(): Int {
                    return newListItems.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    if (listItems.size <= oldItemPosition || newListItems.size <= newItemPosition) {
                        return false
                    }
                    return listItems[oldItemPosition] == newListItems[newItemPosition]
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return true
                }
            })
            listItems = newListItems
            diff.dispatchUpdatesTo(this)
        }

        override fun onClick(v: View) {
            val item = listItems[v.tag as Int]
            if (item.code != app.appOrSystemLanguageCode) {
                app.appLanguageState.addAppLanguageCode(item.code)
            }
            interactionsCount++
            searchingFunnel.logLanguageAdded(true, item.code, currentSearchQuery)
            DeviceUtil.hideSoftKeyboard(this@LanguagesListActivity)
            val returnIntent = Intent()
            returnIntent.putExtra(WikipediaLanguagesFragment.ADD_LANGUAGE_INTERACTIONS, interactionsCount)
            returnIntent.putExtra(LANGUAGE_SEARCHED, isLanguageSearched)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    private inner class HeaderViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(listItem: LanguagesListViewModel.LanguageListItem) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = listItem.code
        }
    }

    private inner class LanguagesListItemHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(listItem: LanguagesListViewModel.LanguageListItem) {
            val languageCode = listItem.code
            itemView.findViewById<TextView>(R.id.localized_language_name).text =
                app.appLanguageState.getAppLanguageLocalizedName(languageCode).orEmpty().capitalize(Locale.getDefault())
            val canonicalName = viewModel.getCanonicalName(languageCode)
            if (binding.languagesListLoadProgress.visibility != View.VISIBLE) {
                itemView.findViewById<TextView>(R.id.language_subtitle).text =
                    if (canonicalName.isNullOrEmpty()) app.appLanguageState.getAppLanguageCanonicalName(languageCode) else canonicalName
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        const val LANGUAGE_SEARCHED = "language_searched"
    }
}
