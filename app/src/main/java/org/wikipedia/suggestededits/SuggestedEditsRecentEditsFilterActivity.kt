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
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.ActivitySuggestedEditsRecentEditsFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.views.DefaultViewHolder

class SuggestedEditsRecentEditsFilterActivity : BaseActivity() {

    private lateinit var binding: ActivitySuggestedEditsRecentEditsFiltersBinding
    private var appLanguagesPreFilterList = mutableListOf<String>()
    private val appLanguagesList get() = WikipediaApp.instance.languageState.appLanguageCodes

    private val languageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val addedCode = appLanguagesList.asSequence().minus(appLanguagesPreFilterList.toSet()).map { it }.toList().toString()
        PatrollerExperienceEvent.logAction("filters_state_change", "pt_filters",
            PatrollerExperienceEvent.getActionDataString(appLanguageCodeAdded = addedCode, appLanguageCodes = appLanguagesList.toString()))
        appLanguagesPreFilterList.clear()

        if (!appLanguagesList.contains(Prefs.recentEditsWikiCode)) {
            Prefs.recentEditsWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
        }

        setUpRecyclerView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuggestedEditsRecentEditsFiltersBinding.inflate(layoutInflater)
        setResult(RESULT_OK)
        setUpRecyclerView()
        setContentView(binding.root)
        PatrollerExperienceEvent.logAction("settings_impression", "pt_filters")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recent_edits_filter, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val defaultTypeSet = SuggestedEditsRecentEditsFilterTypes.DEFAULT_FILTER_TYPE_SET.map { it.id }
        val shouldShowResetButton = !(Prefs.recentEditsIncludedTypeCodes.containsAll(defaultTypeSet) &&
                Prefs.recentEditsIncludedTypeCodes.subtract(defaultTypeSet.toSet()).isEmpty())
        menu.findItem(R.id.menu_filter_reset).isVisible = shouldShowResetButton
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_filter_reset -> {
                PatrollerExperienceEvent.logAction("filters_reset", "pt_filters")
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
        // TODO: limit to the primary language now.
//        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_wiki_filter_header))
//        appLanguagesList.forEach {
//            filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, it))
//        }
//        filterListWithHeaders.add(getString(R.string.notifications_filter_update_app_languages))
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_user_status_header))
        SuggestedEditsRecentEditsFilterTypes.USER_REGISTRATION_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id, true))
        }
        SuggestedEditsRecentEditsFilterTypes.USER_EXPERIENCE_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id, true))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_latest_revisions_header))
        SuggestedEditsRecentEditsFilterTypes.LATEST_REVISIONS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_automated_contributions_header))
        SuggestedEditsRecentEditsFilterTypes.BOT_EDITS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id))
        }
        // TODO: rename to "revert risk":
        filterListWithHeaders.add(getString(R.string.patroller_tasks_filters_contribution_quality_header))
        SuggestedEditsRecentEditsFilterTypes.DAMAGING_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id, true))
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

    private inner class RecentEditsFilterItemViewAddLanguageViewHolder constructor(private val filterItemView: SuggestedEditsRecentEditsFilterItemView) :
            DefaultViewHolder<SuggestedEditsRecentEditsFilterItemView>(filterItemView), SuggestedEditsRecentEditsFilterItemView.Callback {

        fun bindItem(text: String) {
            filterItemView.callback = this
            filterItemView.setSingleLabel(text)
        }

        override fun onCheckedChanged(filter: Filter?) {
            appLanguagesPreFilterList.addAll(appLanguagesList)
            languageChooserLauncher.launch(WikipediaLanguagesActivity.newIntent(this@SuggestedEditsRecentEditsFilterActivity, Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS))
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
                    PatrollerExperienceEvent.logAction("filters_state_change", "pt_filters",
                        PatrollerExperienceEvent.getActionDataString(filterWiki = it.filterCode, appLanguageCodes = appLanguagesList.toString()))
                    Prefs.recentEditsWikiCode = it.filterCode
                } else if (filter.type == FILTER_TYPE_CATEGORY) {
                    PatrollerExperienceEvent.logAction("filters_state_change", "pt_filters",
                        PatrollerExperienceEvent.getActionDataString(filterSelected = it.filterCode, filtersList = Prefs.recentEditsIncludedTypeCodes.toString()))
                    if (filter.isCheckBox) {
                        if (includedTypeCodes.contains(filter.filterCode)) includedTypeCodes.remove(filter.filterCode)
                        else includedTypeCodes.add(filter.filterCode)
                    } else {
                        val group = SuggestedEditsRecentEditsFilterTypes.findGroup(filter.filterCode)
                        includedTypeCodes.removeAll(group.map { type -> type.id }.toSet())
                        // Find the group belongs to it and remove others.
                        includedTypeCodes.add(filter.filterCode)
                    }
                }
                Prefs.recentEditsIncludedTypeCodes = includedTypeCodes
                notifyItemRangeChanged(0, itemCount)
                invalidateOptionsMenu()
            }
        }
    }

    inner class Filter constructor(val type: Int, val filterCode: String, val isCheckBox: Boolean = false) {
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
