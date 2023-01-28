package org.wikipedia.watchlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityWatchlistFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.views.DefaultViewHolder

class WatchlistFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityWatchlistFiltersBinding

    private val languageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        setUpRecyclerView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistFiltersBinding.inflate(layoutInflater)
        setResult(RESULT_OK)
        setUpRecyclerView()
        setContentView(binding.root)
    }

    private fun setUpRecyclerView() {
        binding.watchlistFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.watchlistFiltersRecyclerView.adapter = WatchlistFilterAdapter(this, filterListWithHeaders())
        binding.watchlistFiltersRecyclerView.itemAnimator = null
    }

    private fun filterListWithHeaders(): List<Any> {
        val filterListWithHeaders = mutableListOf<Any>()
        filterListWithHeaders.add(getString(R.string.watchlist_filter_wiki_filter_header))
        filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, getString(R.string.watchlist_filter_all_wikis_text)))
        WikipediaApp.instance.languageState.appLanguageCodes.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, it))
        }
        filterListWithHeaders.add(getString(R.string.notifications_filter_update_app_languages))
        filterListWithHeaders.add(getString(R.string.watchlist_filter_watchlist_activity_header))
        WatchlistFilterTypes.UNSEEN_CHANGES_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.watchlist_filter_automated_contributions_header))
        WatchlistFilterTypes.BOT_EDITS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.watchlist_filter_significance_header))
        WatchlistFilterTypes.MINOR_EDITS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.watchlist_filter_type_of_change_header))
        WatchlistFilterTypes.TYPE_OF_CHANGES_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        return filterListWithHeaders
    }

    class WatchlistFilterItemViewHolder constructor(itemView: WatchlistFilterItemView) :
        DefaultViewHolder<WatchlistFilterItemView>(itemView) {
        fun bindItem(filter: Filter) {
            view.setContents(filter)
        }
    }

    class WatchlistFilterHeaderViewHolder constructor(itemView: View) :
        DefaultViewHolder<View>(itemView) {
        private val headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(filterHeader: String) {
            headerText.text = filterHeader
        }
    }

    private inner class WatchlistFilterItemViewAddLanguageViewHolder constructor(itemView: WatchlistFilterItemView) :
            DefaultViewHolder<WatchlistFilterItemView>(itemView), WatchlistFilterItemView.Callback {

        fun bindItem(text: String) {
            (itemView as WatchlistFilterItemView).callback = this
            itemView.setSingleLabel(text)
        }

        override fun onCheckedChanged(filter: Filter?) {
            languageChooserLauncher.launch(WikipediaLanguagesActivity.newIntent(this@WatchlistFilterActivity, Constants.InvokeSource.WATCHLIST_FILTER_ACTIVITY))
        }
    }

    private inner class WatchlistFilterAdapter(val context: Context, private val filtersList: List<Any>) :
        RecyclerView.Adapter<DefaultViewHolder<*>>(), WatchlistFilterItemView.Callback {
        private var excludedWikiCodes = Prefs.watchlistExcludedWikiCodes.toMutableSet()
        private var includedWikiCodes = Prefs.watchlistIncludedTypeCodes.toMutableSet()

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    WatchlistFilterHeaderViewHolder(layoutInflater.inflate(R.layout.view_watchlist_filter_header, parent, false))
                }
                VIEW_TYPE_ADD_LANGUAGE -> {
                    WatchlistFilterItemViewAddLanguageViewHolder(WatchlistFilterItemView(context))
                }
                else -> {
                    val watchlistFilterItemView = WatchlistFilterItemView(context)
                    watchlistFilterItemView.callback = this
                    WatchlistFilterItemViewHolder(watchlistFilterItemView)
                }
            }
        }

        override fun getItemCount(): Int {
            return filtersList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (filtersList[position] is String && filtersList[position] == getString(R.string.notifications_filter_update_app_languages)) VIEW_TYPE_ADD_LANGUAGE
            else if (filtersList[position] is String) VIEW_TYPE_HEADER
            else VIEW_TYPE_ITEM
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, position: Int) {
            when (holder) {
                is WatchlistFilterHeaderViewHolder -> holder.bindItem(filtersList[position] as String)
                is WatchlistFilterItemViewAddLanguageViewHolder -> holder.bindItem(filtersList[position] as String)
                else -> (holder as WatchlistFilterItemViewHolder).bindItem(filtersList[position] as Filter)
            }
        }

        override fun onCheckedChanged(filter: Filter?) {
            when (filter!!.filterCode) {
                context.getString(R.string.watchlist_filter_all_wikis_text) -> {
                    if (excludedWikiCodes.isEmpty()) {
                        excludedWikiCodes.addAll(WikipediaApp.instance.languageState.appLanguageCodes)
                    } else {
                        excludedWikiCodes.clear()
                    }
                }
                else -> {
                    if (filter.type == FILTER_TYPE_WIKI) {
                        if (excludedWikiCodes.contains(filter.filterCode)) excludedWikiCodes.remove(filter.filterCode)
                        else excludedWikiCodes.add(filter.filterCode)
                    } else if (filter.type == Companion.FILTER_TYPE_CATEGORY) {
                        if (includedWikiCodes.contains(filter.filterCode)) includedWikiCodes.remove(filter.filterCode)
                        else includedWikiCodes.add(filter.filterCode)
                    }
                }
            }
            Prefs.watchlistExcludedWikiCodes = excludedWikiCodes
            Prefs.watchlistIncludedTypeCodes = includedWikiCodes
            notifyItemRangeChanged(0, itemCount)
        }
    }

    inner class Filter constructor(val type: Int, val filterCode: String) {
        fun isEnabled(): Boolean {
            val excludedWikiCodes = Prefs.watchlistExcludedWikiCodes
            if (filterCode == getString(R.string.notifications_all_wikis_text)) {
                return WikipediaApp.instance.languageState.appLanguageCodes.find { excludedWikiCodes.contains(it) } == null
            }
            return !excludedWikiCodes.contains(filterCode) || Prefs.watchlistIncludedTypeCodes.contains(filterCode)
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_ADD_LANGUAGE = 2
        const val FILTER_TYPE_WIKI = 0
        const val FILTER_TYPE_CATEGORY = 1

        fun newIntent(context: Context): Intent {
            return Intent(context, WatchlistFilterActivity::class.java)
        }
    }
}
