package org.wikipedia.places

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.databinding.ActivityPlacesFiltersBinding
import org.wikipedia.databinding.ViewPlacesFilterItemBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder

class PlacesFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityPlacesFiltersBinding
    private var initLanguage: String = Prefs.placesWikiCode
    private var filtersList = mutableListOf<String>()

    val addLanguageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Check if places wiki language code was deleted
        if (!WikipediaApp.instance.languageState.appLanguageCodes.contains(Prefs.placesWikiCode)) {
            Prefs.placesWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
        }
        setUpRecyclerView()
        binding.placesFiltersRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlacesFiltersBinding.inflate(layoutInflater)

        setUpRecyclerView()
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.background_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.background_color))
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(binding.root)
    }

    override fun finish() {
        setResult(RESULT_OK, Intent()
            .putExtra(EXTRA_LANG_CHANGED, initLanguage != Prefs.placesWikiCode))
        super.finish()
    }

    private fun setUpRecyclerView() {
        filtersList.clear()
        filtersList.add(HEADER)
        filtersList.addAll(WikipediaApp.instance.languageState.appLanguageCodes)
        filtersList.add(FOOTER)
        binding.placesFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.placesFiltersRecyclerView.adapter = PlacesLangListFilterAdapter()
    }

    private inner class PlacesLangListFilterAdapter :
        RecyclerView.Adapter<DefaultViewHolder<*>>(), PlacesFilterItemViewHolder.Callback {

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    PlacesFilterHeaderViewHolder(layoutInflater.inflate(R.layout.view_watchlist_filter_header, parent, false))
                }
                VIEW_TYPE_FOOTER -> {
                    PlacesFilterFooterViewHolder(layoutInflater.inflate(R.layout.view_places_filters_footer, parent, false))
                }
                else -> {
                    PlacesFilterItemViewHolder(ViewPlacesFilterItemBinding.inflate(layoutInflater), this)
                }
            }
        }

        override fun getItemCount(): Int {
            return filtersList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (filtersList[position] == HEADER) VIEW_TYPE_HEADER
            else if (filtersList[position] == FOOTER) VIEW_TYPE_FOOTER
            else VIEW_TYPE_ITEM
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<*>, position: Int) {
            when (holder) {
                is PlacesFilterHeaderViewHolder -> holder.bindItem(getString(R.string.watchlist_filter_wiki_filter_header))
                is PlacesFilterFooterViewHolder -> holder.bindItem()
                else -> (holder as PlacesFilterItemViewHolder).bindItem(filtersList[position])
            }
        }

        override fun onLangSelected() {
            notifyDataSetChanged()
        }
    }

    inner class PlacesFilterHeaderViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        private val headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(filterHeader: String) {
            headerText.setTextColor(ResourceUtil.getThemedColor(this@PlacesFilterActivity, R.attr.primary_color))
            headerText.text = filterHeader
        }
    }
    inner class PlacesFilterFooterViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        fun bindItem() {
            itemView.setOnClickListener {
                addLanguageLauncher.launch(WikipediaLanguagesActivity.newIntent(itemView.context,
                    Constants.InvokeSource.PLACES))
            }
        }
    }
    class PlacesFilterItemViewHolder(private val itemViewBinding: ViewPlacesFilterItemBinding, val callback: Callback) : DefaultViewHolder<View>(itemViewBinding.root) {
        interface Callback {
            fun onLangSelected()
        }

        fun bindItem(languageCode: String) {
            itemViewBinding.placesFilterTitle.text = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(languageCode)
            itemViewBinding.placesFilterLangCode.setLangCode(languageCode)
            itemViewBinding.placesFilterRadio.isVisible = languageCode == Prefs.placesWikiCode
            itemViewBinding.root.setOnClickListener {
                if (languageCode != Prefs.placesWikiCode) {
                    PlacesEvent.logAction("filter_change_save", "filter_view")
                }
                Prefs.placesWikiCode = languageCode
                callback.onLangSelected()
            }
        }
    }
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FOOTER = 1
        private const val VIEW_TYPE_ITEM = 2
        private const val HEADER = "header"
        private const val FOOTER = "footer"
        const val EXTRA_LANG_CHANGED = "langChanged"
        fun newIntent(context: Context): Intent {
            return Intent(context, PlacesFilterActivity::class.java)
        }
    }
}
