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
        setResult(ACTIVITY_RESULT_LANGUAGES_CHANGED)
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
        // TODO: add headers
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
        private var excludedWikiCodes = Prefs.notificationExcludedWikiCodes.toMutableSet()
        private var excludedTypeCodes = Prefs.notificationExcludedTypeCodes.toMutableSet()

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    WatchlistFilterHeaderViewHolder(layoutInflater.inflate(R.layout.view_notification_filter_header, parent, false))
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
                context.getString(R.string.notifications_all_types_text) -> {
                    if (excludedTypeCodes.isEmpty()) {
                        excludedTypeCodes.addAll(allTypesIdList())
                    } else {
                        excludedTypeCodes.clear()
                    }
                }
                context.getString(R.string.notifications_all_wikis_text) -> {
                    if (excludedWikiCodes.isEmpty()) {
                        excludedWikiCodes.addAll(allWikisList())
                    } else {
                        excludedWikiCodes.clear()
                    }
                }
                else -> {
                    if (filter.type == FILTER_TYPE_WIKI) {
                        if (excludedWikiCodes.contains(filter.filterCode)) excludedWikiCodes.remove(filter.filterCode)
                        else excludedWikiCodes.add(filter.filterCode)
                    } else if (filter.type == Companion.FILTER_TYPE_CATEGORY) {
                        if (excludedTypeCodes.contains(filter.filterCode)) excludedTypeCodes.remove(filter.filterCode)
                        else excludedTypeCodes.add(filter.filterCode)
                    }
                }
            }
            Prefs.notificationExcludedWikiCodes = excludedWikiCodes
            Prefs.notificationExcludedTypeCodes = excludedTypeCodes
            notifyItemRangeChanged(0, itemCount)
        }
    }

    inner class Filter constructor(val type: Int, val filterCode: String, val imageRes: Int? = null) {
        fun isEnabled(): Boolean {
            val excludedWikiCodes = Prefs.notificationExcludedWikiCodes
            val excludedTypeCodes = Prefs.notificationExcludedTypeCodes
            if (filterCode == getString(R.string.notifications_all_types_text)) {
                return allTypesIdList().find { excludedTypeCodes.contains(it) } == null
            }
            if (filterCode == getString(R.string.notifications_all_wikis_text)) {
                return allWikisList().find { excludedWikiCodes.contains(it) } == null
            }
            return !excludedWikiCodes.contains(filterCode) && !excludedTypeCodes.contains(filterCode)
        }
    }

    companion object {
        const val ACTIVITY_RESULT_LANGUAGES_CHANGED = 2
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_ADD_LANGUAGE = 2
        private const val FILTER_TYPE_WIKI = 0
        private const val FILTER_TYPE_CATEGORY = 1

        fun allWikisList(): List<String> {
            val wikiList = mutableListOf<String>()
            wikiList.addAll(WikipediaApp.instance.languageState.appLanguageCodes)
            wikiList.add(Constants.WIKI_CODE_COMMONS)
            wikiList.add(Constants.WIKI_CODE_WIKIDATA)
            return wikiList
        }

        fun allTypesIdList(): List<String> {
            return NotificationCategory.FILTERS_GROUP.map { it.id }
        }

        fun newIntent(context: Context): Intent {
            return Intent(context, WatchlistFilterActivity::class.java)
        }
    }
}
