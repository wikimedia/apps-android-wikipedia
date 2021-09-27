package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityNotificationsFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DefaultViewHolder

class NotificationsFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsFiltersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsFiltersBinding.inflate(layoutInflater)
        setUpRecyclerView()
        setContentView(binding.root)
    }

    private fun setUpRecyclerView() {
        binding.notificationsFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsFiltersRecyclerView.adapter = NotificationsFilterAdapter(this, filterListWithHeaders())
    }

    private fun filterListWithHeaders(): MutableList<Any> {
        val filterListWithHeaders = mutableListOf<Any>()
        filterListWithHeaders.add(getString(R.string.notifications_wiki_filter_header))
        filterListWithHeaders.add(Filter(getString(R.string.notifications_all_wikis_text)))
        WikipediaApp.getInstance().language().appLanguageCodes.forEach {
            filterListWithHeaders.add(Filter(it, null))
        }
        filterListWithHeaders.add(Filter("commons", R.drawable.ic_commons_logo))
        filterListWithHeaders.add(Filter("wikidata", R.drawable.ic_wikidata_logo))
        filterListWithHeaders.add(getString(R.string.notifications_type_filter_header))
        filterListWithHeaders.add(Filter(getString(R.string.notifications_all_types_text)))
        NotificationCategory.FILTERS_GROUP.forEach {
            filterListWithHeaders.add(Filter(it.id, it.iconResId))
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
        var headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(filterHeader: String) {
            headerText.text = filterHeader
        }
    }

    class NotificationsFilterAdapter(val context: Context, private val filtersList: MutableList<Any>) :
        RecyclerView.Adapter<DefaultViewHolder<*>>(), NotificationFilterItemView.Callback {
        var app: WikipediaApp = WikipediaApp.getInstance()
        private var filteredWikisList = mutableListOf<String>()

        init {
            if (Prefs.notificationsFilterLanguageCodes == null) addAllWikiAndTypeFilters()
            else filteredWikisList.addAll(StringUtil.csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty()))
        }

        private fun addAllWikiAndTypeFilters() {
            val allWikiAndTypeList = mutableListOf<String>()
            allWikiAndTypeList.addAll(allWikisList())
            allWikiAndTypeList.addAll(allTypesIdList())
            Prefs.notificationsFilterLanguageCodes = StringUtil.listToCsv(allWikiAndTypeList)
            filteredWikisList.clear()
            filteredWikisList.addAll(allWikiAndTypeList)
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return if (type == VIEW_TYPE_HEADER) {
                val view = LayoutInflater.from(context).inflate(R.layout.view_notification_filter_header, parent, false)
                NotificationFilterHeaderViewHolder(view)
            } else {
                val notificationsFilterItemView = NotificationFilterItemView(context)
                notificationsFilterItemView.callback = this
                NotificationFilterItemViewHolder(notificationsFilterItemView)
            }
        }

        override fun getItemCount(): Int {
            return filtersList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (filtersList[position] is String) VIEW_TYPE_HEADER
            else VIEW_TYPE_ITEM
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, position: Int) {
            if (holder is NotificationFilterHeaderViewHolder) holder.bindItem(filtersList[position] as String)
            else (holder as NotificationFilterItemViewHolder).bindItem(filtersList[position] as Filter)
        }

        override fun onCheckedChanged(langCode: String) {
            if (langCode == context.getString(R.string.notifications_all_types_text)) {
                allTypesIdList().filter { !filteredWikisList.contains(it) }
                    .forEach { typeId -> filteredWikisList.add(typeId) }
            } else if (langCode == context.getString(R.string.notifications_all_wikis_text)) {
                allWikisList().filter { !filteredWikisList.contains(it) }.forEach { category -> filteredWikisList.add(category) }
            } else {
                if (filteredWikisList.contains(langCode)) {
                    filteredWikisList.remove(langCode)
                } else {
                    filteredWikisList.add(langCode)
                }
            }
            Prefs.notificationsFilterLanguageCodes = StringUtil.listToCsv(filteredWikisList)
            notifyDataSetChanged()
        }
    }

    class Filter constructor(val languageCode: String, val imageRes: Int? = null) {
        fun isEnabled(): Boolean {
            val list = StringUtil.csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty())

            if (languageCode == WikipediaApp.getInstance().getString(R.string.notifications_all_types_text)) {
                return list.containsAll(allTypesIdList())
            }
            if (languageCode == WikipediaApp.getInstance().getString(R.string.notifications_all_wikis_text)) {
                return list.containsAll(allWikisList())
            }
            return list.contains(languageCode)
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        fun allWikisList():List<String>{
            val wikiList = mutableListOf<String>()
            wikiList.addAll(WikipediaApp.getInstance().language().appLanguageCodes)
            wikiList.add("commons")
            wikiList.add("wikidata")
            return wikiList
        }

        fun allTypesIdList():List<String>{
            val typeList = mutableListOf<String>()
            NotificationCategory.FILTERS_GROUP.forEach { typeList.add(it.id) }
            return typeList
        }

        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsFilterActivity::class.java)
        }
    }
}
