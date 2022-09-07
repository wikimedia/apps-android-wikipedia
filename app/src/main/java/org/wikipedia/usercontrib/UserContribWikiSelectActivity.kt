package org.wikipedia.usercontrib

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityUserContribWikiSelectBinding
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.views.DefaultViewHolder

class UserContribWikiSelectActivity : BaseActivity() {

    private lateinit var binding: ActivityUserContribWikiSelectBinding
    private lateinit var selectLangCode: String

    private val langUpdateLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        binding.recyclerView.adapter = WikiSelectAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserContribWikiSelectBinding.inflate(layoutInflater)
        selectLangCode = intent.getStringExtra(INTENT_EXTRA_SELECT_LANG_CODE).orEmpty().ifEmpty { WikipediaApp.instance.appOrSystemLanguageCode }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = WikiSelectAdapter(this)
        setContentView(binding.root)
    }

    inner class WikiSelectItemViewHolder constructor(itemView: UserContribWikiSelectItemView) :
        DefaultViewHolder<UserContribWikiSelectItemView>(itemView) {
        fun bindItem(item: Item?) {
            view.setContents(item!!, item.itemCode == selectLangCode)
        }
    }

    private inner class WikiSelectAddLanguageViewHolder constructor(itemView: UserContribWikiSelectItemView) :
            DefaultViewHolder<UserContribWikiSelectItemView>(itemView), UserContribWikiSelectItemView.Callback {
        fun bindItem(text: String) {
            (itemView as UserContribWikiSelectItemView).callback = this
            itemView.setSingleLabel(text)
        }

        override fun onSelected(item: Item?) {
            langUpdateLauncher.launch(WikipediaLanguagesActivity.newIntent(this@UserContribWikiSelectActivity, Constants.InvokeSource.USER_CONTRIB_ACTIVITY))
        }
    }

    private inner class WikiSelectAdapter(val context: Context) : RecyclerView.Adapter<DefaultViewHolder<*>>(), UserContribWikiSelectItemView.Callback {
        private val itemList = mutableListOf<Any>()

        init {
            WikipediaApp.instance.languageState.appLanguageCodes.forEach { itemList.add(Item(it, null)) }
            itemList.add(Item(Constants.WIKI_CODE_COMMONS, R.drawable.ic_commons_logo))
            itemList.add(Item(Constants.WIKI_CODE_WIKIDATA, R.drawable.ic_wikidata_logo))
            itemList.add(getString(R.string.notifications_filter_update_app_languages))
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_ADD_LANGUAGE -> {
                    WikiSelectAddLanguageViewHolder(UserContribWikiSelectItemView(context))
                }
                else -> {
                    val view = UserContribWikiSelectItemView(context)
                    view.callback = this
                    WikiSelectItemViewHolder(view)
                }
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (itemList[position] is String && itemList[position] == getString(R.string.notifications_filter_update_app_languages)) VIEW_TYPE_ADD_LANGUAGE
            else VIEW_TYPE_ITEM
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, position: Int) {
            when (holder) {
                is WikiSelectAddLanguageViewHolder -> holder.bindItem(itemList[position] as String)
                else -> (holder as WikiSelectItemViewHolder).bindItem(itemList[position] as Item)
            }
        }

        override fun onSelected(item: Item?) {
            setResult(RESULT_OK, intent.putExtra(INTENT_EXTRA_SELECT_LANG_CODE, item?.itemCode))
            finish()
        }
    }

    inner class Item constructor(val itemCode: String, val imageRes: Int? = null)

    companion object {
        const val INTENT_EXTRA_SELECT_LANG_CODE = "selectLangCode"
        const val ACTIVITY_RESULT_LANGUAGES_CHANGED = 2

        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_ADD_LANGUAGE = 2

        fun newIntent(context: Context, selectLangCode: String): Intent {
            return Intent(context, UserContribWikiSelectActivity::class.java)
                    .putExtra(INTENT_EXTRA_SELECT_LANG_CODE, selectLangCode)
        }
    }
}
