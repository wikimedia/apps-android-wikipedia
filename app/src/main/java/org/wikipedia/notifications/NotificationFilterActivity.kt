package org.wikipedia.notifications

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
import org.wikipedia.databinding.ActivityNotificationsFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.views.DefaultViewHolder

class NotificationFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsFiltersBinding

    private val languageChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        setResult(ACTIVITY_RESULT_LANGUAGES_CHANGED)
        setUpRecyclerView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsFiltersBinding.inflate(layoutInflater)
        setResult(RESULT_OK)
        setUpRecyclerView()
        setContentView(binding.root)
    }

    private fun setUpRecyclerView() {
        binding.notificationsFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsFiltersRecyclerView.adapter = NotificationsFilterAdapter(this, filterListWithHeaders())
        binding.notificationsFiltersRecyclerView.itemAnimator = null
    }

    private fun filterListWithHeaders(): List<Any> {
        val filterListWithHeaders = mutableListOf<Any>()
        filterListWithHeaders.add(getString(R.string.notifications_wiki_filter_header))
        filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, getString(R.string.notifications_all_wikis_text)))
        WikipediaApp.instance.languageState.appLanguageCodes.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, it, null))
        }
        filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, Constants.WIKI_CODE_COMMONS, R.drawable.ic_commons_logo))
        filterListWithHeaders.add(Filter(FILTER_TYPE_WIKI, Constants.WIKI_CODE_WIKIDATA, R.drawable.ic_wikidata_logo))
        filterListWithHeaders.add(getString(R.string.notifications_filter_update_app_languages))
        filterListWithHeaders.add(getString(R.string.notifications_type_filter_header))
        filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, getString(R.string.notifications_all_types_text)))
        NotificationCategory.FILTERS_GROUP.forEach {
            filterListWithHeaders.add(Filter(FILTER_TYPE_CATEGORY, it.id, it.iconResId))
        }
        return filterListWithHeaders
    }

    class NotificationFilterItemViewHolder constructor(itemView: NotificationFilterItemView) :
        DefaultViewHolder<NotificationFilterItemView>(itemView) {
        fun bindItem(filter: Filter) {
            view.setContents(filter)
        }
    }

    class NotificationFilterHeaderViewHolder constructor(itemView: View) :
        DefaultViewHolder<View>(itemView) {
        private val headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(filterHeader: String) {
            headerText.text = filterHeader
        }
    }

    private inner class NotificationFilterAddLanguageViewHolder constructor(private val filterItemView: NotificationFilterItemView) :
            DefaultViewHolder<NotificationFilterItemView>(filterItemView), NotificationFilterItemView.Callback {
        fun bindItem(text: String) {
            filterItemView.callback = this
            filterItemView.setSingleLabel(text)
        }

        override fun onCheckedChanged(filter: Filter?) {
            languageChooserLauncher.launch(WikipediaLanguagesActivity.newIntent(this@NotificationFilterActivity, Constants.InvokeSource.NOTIFICATION))
        }
    }

    private inner class NotificationsFilterAdapter(val context: Context, private val filtersList: List<Any>) :
        RecyclerView.Adapter<DefaultViewHolder<*>>(), NotificationFilterItemView.Callback {
        private var excludedWikiCodes = Prefs.notificationExcludedWikiCodes.toMutableSet()
        private var excludedTypeCodes = Prefs.notificationExcludedTypeCodes.toMutableSet()

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    NotificationFilterHeaderViewHolder(layoutInflater.inflate(R.layout.view_notification_filter_header, parent, false))
                }
                VIEW_TYPE_ADD_LANGUAGE -> {
                    NotificationFilterAddLanguageViewHolder(NotificationFilterItemView(context))
                }
                else -> {
                    val notificationsFilterItemView = NotificationFilterItemView(context)
                    notificationsFilterItemView.callback = this
                    NotificationFilterItemViewHolder(notificationsFilterItemView)
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
                is NotificationFilterHeaderViewHolder -> holder.bindItem(filtersList[position] as String)
                is NotificationFilterAddLanguageViewHolder -> holder.bindItem(filtersList[position] as String)
                else -> (holder as NotificationFilterItemViewHolder).bindItem(filtersList[position] as Filter)
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
            return Intent(context, NotificationFilterActivity::class.java)
        }
    }
}
