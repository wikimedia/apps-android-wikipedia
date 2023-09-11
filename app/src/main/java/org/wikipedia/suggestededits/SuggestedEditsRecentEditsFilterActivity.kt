package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import org.wikipedia.databinding.ActivitySuggestedEditsRecentEditsFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.views.DefaultViewHolder

class SuggestedEditsRecentEditsFilterActivity : BaseActivity() {

    private lateinit var binding: ActivitySuggestedEditsRecentEditsFiltersBinding

    private val languageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        setUpRecyclerView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuggestedEditsRecentEditsFiltersBinding.inflate(layoutInflater)
        setResult(RESULT_OK)
        setUpRecyclerView()
        setContentView(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recent_edits_filter, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val shouldShowResetButton = Prefs.recentEditsWikiCode.isNotEmpty() || !Prefs.recentEditsIncludedTypeCodes.containsAll(SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_TYPE_SET.map { it.id })
        menu.findItem(R.id.menu_filter_reset).isVisible = shouldShowResetButton
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_filter_reset -> {
                resetFilterSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpRecyclerView() {
        binding.recentEditsFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recentEditsFiltersRecyclerView.adapter = RecentEditsFilterAdapter(this, filterListWithHeaders())
        binding.recentEditsFiltersRecyclerView.itemAnimator = null
    }

    private fun resetFilterSettings() {
        Prefs.recentEditsWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
        Prefs.recentEditsIncludedTypeCodes = SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_TYPE_SET.map { it.id }
        setUpRecyclerView()
        invalidateOptionsMenu()
     }

    private fun filterListWithHeaders(): List<Any> {
        val filterListWithHeaders = mutableListOf<Any>()
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_wiki_filter_header))
        filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, getString(R.string.patroller_tasks_filters_all_text)))
        WikipediaApp.instance.languageState.appLanguageCodes.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, it))
        }
        filterListWithHeaders.add(getString(R.string.notifications_filter_update_app_languages))
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_user_status_header))
        SuggestedEditsRecentEditsFilterTypes.USER_STATUS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_latest_revisions_header))
        SuggestedEditsRecentEditsFilterTypes.LATEST_REVISIONS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_automated_contributions_header))
        SuggestedEditsRecentEditsFilterTypes.BOT_EDITS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_contribution_quality_header))
        SuggestedEditsRecentEditsFilterTypes.CONTRIBUTION_QUALITY_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_user_intent_header))
        SuggestedEditsRecentEditsFilterTypes.USER_INTENT_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_significance_header))
        SuggestedEditsRecentEditsFilterTypes.MINOR_EDITS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        return filterListWithHeaders
    }

    class RecentEditsFilterItemViewHolder constructor(itemView: SuggestedEditsRecentEditsFilterItemView) :
        DefaultViewHolder<SuggestedEditsRecentEditsFilterItemView>(itemView) {
        fun bindItem(filter: Filter) {
            view.setContents(filter)
        }
    }

    class RecentEditsFilterHeaderViewHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView) {
        private val headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(filterHeader: String) {
            headerText.text = filterHeader
        }
    }

    private inner class RecentEditsFilterItemViewAddLanguageViewHolder constructor(itemView: SuggestedEditsRecentEditsFilterItemView) :
            DefaultViewHolder<SuggestedEditsRecentEditsFilterItemView>(itemView), SuggestedEditsRecentEditsFilterItemView.Callback {

        fun bindItem(text: String) {
            (itemView as SuggestedEditsRecentEditsFilterItemView).callback = this
            itemView.setSingleLabel(text)
        }

        override fun onCheckedChanged(filter: Filter?) {
            languageChooserLauncher.launch(WikipediaLanguagesActivity.newIntent(this@SuggestedEditsRecentEditsFilterActivity, Constants.InvokeSource.RECENT_EDITS_FILTER_ACTIVITY))
        }
    }

    private inner class RecentEditsFilterAdapter(val context: Context, private val filtersList: List<Any>) :
        RecyclerView.Adapter<DefaultViewHolder<*>>(), SuggestedEditsRecentEditsFilterItemView.Callback {
        private var includedTypeCodes = Prefs.recentEditsIncludedTypeCodes.toMutableSet()

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    RecentEditsFilterHeaderViewHolder(layoutInflater.inflate(R.layout.view_recent_edits_filter_header, parent, false))
                }
                VIEW_TYPE_ADD_LANGUAGE -> {
                    RecentEditsFilterItemViewAddLanguageViewHolder(SuggestedEditsRecentEditsFilterItemView(context))
                }
                else -> {
                    val suggestedEditsRecentEditsFilterItemView = SuggestedEditsRecentEditsFilterItemView(context)
                    suggestedEditsRecentEditsFilterItemView.callback = this
                    RecentEditsFilterItemViewHolder(suggestedEditsRecentEditsFilterItemView)
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
                is RecentEditsFilterHeaderViewHolder -> holder.bindItem(filtersList[position] as String)
                is RecentEditsFilterItemViewAddLanguageViewHolder -> holder.bindItem(filtersList[position] as String)
                else -> (holder as RecentEditsFilterItemViewHolder).bindItem(filtersList[position] as Filter)
            }
        }

        override fun onCheckedChanged(filter: Filter?) {
            filter?.let {
                if (it.type == FILTER_TYPE_WIKI) {
                    Prefs.recentEditsWikiCode = it.filterCode
                } else if (filter.type == FILTER_TYPE_CATEGORY) {
                    val group = SuggestedEditsRecentEditsFilterTypes.findGroup(filter.filterCode)
                    includedTypeCodes.removeAll(group.map { type -> type.id }.toSet())
                    // Find the group belongs to it and remove others.
                    includedTypeCodes.add(filter.filterCode)
                }
                Prefs.recentEditsIncludedTypeCodes = includedTypeCodes
                notifyItemRangeChanged(0, itemCount)
                invalidateOptionsMenu()
            }
        }
    }

    inner class Filter constructor(val type: Int, val filterCode: String) {
        fun isEnabled(): Boolean {
            return if (type == FILTER_TYPE_WIKI) {
                Prefs.recentEditsWikiCode == filterCode
            } else {
                Prefs.recentEditsIncludedTypeCodes.contains(filterCode)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_ADD_LANGUAGE = 2
        const val FILTER_TYPE_WIKI = 0
        const val FILTER_TYPE_CATEGORY = 1

        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsRecentEditsFilterActivity::class.java)
        }
    }
}
