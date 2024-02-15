package org.wikipedia.usercontrib

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
import org.wikipedia.databinding.ActivityUserContribWikiSelectBinding
import org.wikipedia.page.Namespace
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.views.DefaultViewHolder

class UserContribFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityUserContribWikiSelectBinding

    private val langUpdateLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!WikipediaApp.instance.languageState.appLanguageCodes.contains(Prefs.userContribFilterLangCode)) {
            Prefs.userContribFilterLangCode = WikipediaApp.instance.appOrSystemLanguageCode
        }
        binding.recyclerView.adapter = ItemAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserContribWikiSelectBinding.inflate(layoutInflater)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ItemAdapter(this)
        binding.recyclerView.itemAnimator = null
        setContentView(binding.root)
        setResult(RESULT_OK)
    }

    inner class ItemViewHolder constructor(itemView: UserContribFilterItemView) :
        DefaultViewHolder<UserContribFilterItemView>(itemView) {
        fun bindItem(item: Item) {
            view.setContents(item)
        }
    }

    private inner class FilterHeaderViewHolder constructor(itemView: View) :
        DefaultViewHolder<View>(itemView) {
        val headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(filterHeader: String) {
            headerText.text = filterHeader
        }
    }

    private inner class AddLanguageViewHolder constructor(private val filterItemView: UserContribFilterItemView) :
            DefaultViewHolder<UserContribFilterItemView>(filterItemView), UserContribFilterItemView.Callback {
        fun bindItem(text: String) {
            filterItemView.callback = this
            filterItemView.setSingleLabel(text)
        }

        override fun onSelected(item: Item?) {
            langUpdateLauncher.launch(WikipediaLanguagesActivity.newIntent(this@UserContribFilterActivity, Constants.InvokeSource.USER_CONTRIB_ACTIVITY))
        }
    }

    private inner class ItemAdapter(val context: Context) : RecyclerView.Adapter<DefaultViewHolder<*>>(), UserContribFilterItemView.Callback {
        private val itemList = mutableListOf<Any>()

        init {
            itemList.add(getString(R.string.notifications_wiki_filter_header))
            WikipediaApp.instance.languageState.appLanguageCodes.forEach { itemList.add(Item(FILTER_TYPE_WIKI, it, null)) }
            itemList.add(Item(FILTER_TYPE_WIKI, Constants.WIKI_CODE_COMMONS, R.drawable.ic_commons_logo))
            itemList.add(Item(FILTER_TYPE_WIKI, Constants.WIKI_CODE_WIKIDATA, R.drawable.ic_wikidata_logo))
            itemList.add(getString(R.string.notifications_filter_update_app_languages))
            itemList.add(getString(R.string.user_contrib_filter_ns_header))
            itemList.add(Item(FILTER_TYPE_NAMESPACE, getString(R.string.user_contrib_filter_all), null))
            itemList.add(Item(FILTER_TYPE_NAMESPACE, getString(R.string.namespace_article), R.drawable.ic_article_ltr_ooui))
            itemList.add(Item(FILTER_TYPE_NAMESPACE, TalkAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode), R.drawable.ic_notification_article_talk))
            itemList.add(Item(FILTER_TYPE_NAMESPACE, UserAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode), R.drawable.ic_user_avatar))
            itemList.add(Item(FILTER_TYPE_NAMESPACE, UserTalkAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode), R.drawable.ic_notification_user_talk))
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    FilterHeaderViewHolder(layoutInflater.inflate(R.layout.view_notification_filter_header, parent, false))
                }
                VIEW_TYPE_ADD_LANGUAGE -> {
                    AddLanguageViewHolder(UserContribFilterItemView(context))
                }
                else -> {
                    val view = UserContribFilterItemView(context)
                    view.callback = this
                    ItemViewHolder(view)
                }
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (itemList[position] is String && itemList[position] == getString(R.string.notifications_filter_update_app_languages)) VIEW_TYPE_ADD_LANGUAGE
            else if (itemList[position] is String) VIEW_TYPE_HEADER
            else VIEW_TYPE_ITEM
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, position: Int) {
            when (holder) {
                is FilterHeaderViewHolder -> holder.bindItem(itemList[position] as String)
                is AddLanguageViewHolder -> holder.bindItem(itemList[position] as String)
                else -> (holder as ItemViewHolder).bindItem(itemList[position] as Item)
            }
        }

        override fun onSelected(item: Item?) {
            item?.let {
                if (it.type == FILTER_TYPE_WIKI) {
                    Prefs.userContribFilterLangCode = item.filterCode
                } else if (it.type == FILTER_TYPE_NAMESPACE) {
                    var excludedNsFilter = Prefs.userContribFilterExcludedNs
                    when (val namespaceCode = getNamespaceCode(item.filterCode)) {
                        -1 -> { // Select "all"
                            excludedNsFilter = if (excludedNsFilter.isEmpty() || excludedNsFilter.size < NAMESPACE_LIST.size) {
                                NAMESPACE_LIST.toSet()
                            } else {
                                emptySet()
                            }
                        }
                        else -> {
                            excludedNsFilter = if (excludedNsFilter.contains(namespaceCode)) {
                                excludedNsFilter.minus(namespaceCode)
                            } else {
                                excludedNsFilter.plus(namespaceCode)
                            }
                        }
                    }
                    Prefs.userContribFilterExcludedNs = excludedNsFilter
                }
            }
            notifyItemRangeChanged(0, itemCount)
        }
    }

    private fun getNamespaceCode(text: String): Int {
        return when (text) {
            getString(R.string.namespace_article) -> Namespace.MAIN.code()
            TalkAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode) -> Namespace.TALK.code()
            UserAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode) -> Namespace.USER.code()
            UserTalkAliasData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode) -> Namespace.USER_TALK.code()
            else -> -1
        }
    }

    inner class Item constructor(val type: Int, val filterCode: String, val imageRes: Int? = null) {
        fun isEnabled(): Boolean {
            if (type == FILTER_TYPE_WIKI) {
                return Prefs.userContribFilterLangCode == filterCode
            }
            val excludedNsFilter = Prefs.userContribFilterExcludedNs
            if (filterCode == getString(R.string.user_contrib_filter_all)) {
                return NAMESPACE_LIST.find { excludedNsFilter.contains(it) } == null
            }
            return !excludedNsFilter.contains(getNamespaceCode(filterCode))
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_ADD_LANGUAGE = 2
        const val FILTER_TYPE_WIKI = 0
        const val FILTER_TYPE_NAMESPACE = 1
        val NAMESPACE_LIST = listOf(Namespace.MAIN.code(), Namespace.TALK.code(), Namespace.USER.code(), Namespace.USER_TALK.code())

        fun newIntent(context: Context): Intent {
            return Intent(context, UserContribFilterActivity::class.java)
        }
    }
}
