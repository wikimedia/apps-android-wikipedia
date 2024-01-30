package org.wikipedia.places

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityPlacesFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.LangCodeView

class PlacesFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityPlacesFiltersBinding
    private var initLanguage: String = Prefs.placesWikiCode
    val filtersList: List<String>
        get() {
            val list = mutableListOf<String>()
            list.add(HEADER)
            list.addAll(appLanguageCodes)
            list.add(FOOTER)
            return list
        }

    val appLanguageCodes: List<String>
        get() {
            return WikipediaApp.instance.languageState.appLanguageCodes
        }

    val addLanguageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Check if places wiki language code was deleted
        if (!WikipediaApp.instance.languageState.appLanguageCodes.contains(Prefs.placesWikiCode)) {
            Prefs.placesWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
        }
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
        binding.placesFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.placesFiltersRecyclerView.adapter = PlacesLangListFilterAdapter(this)
    }

    private inner class PlacesLangListFilterAdapter(val context: Context) :
        RecyclerView.Adapter<DefaultViewHolder<*>>(), PlacesFilterItemViewHolder.Callback {

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): DefaultViewHolder<*> {
            return when (type) {
                VIEW_TYPE_HEADER -> {
                    PlacesFilterHeaderViewHolder(
                        layoutInflater.inflate(
                            R.layout.view_watchlist_filter_header,
                            parent,
                            false
                        )
                    )
                }
                VIEW_TYPE_FOOTER -> {
                    PlacesFilterFooterViewHolder(
                        layoutInflater.inflate(
                            R.layout.view_places_filters_footer,
                            parent,
                            false
                        )
                    )
                }
                else -> {
                    PlacesFilterItemViewHolder(
                        layoutInflater.inflate(
                            R.layout.view_places_filter_item,
                            parent,
                            false
                        ), this
                    )
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
                is PlacesFilterHeaderViewHolder -> holder.bindItem(
                    context, getString(R.string.watchlist_filter_wiki_filter_header)
                )
                is PlacesFilterFooterViewHolder -> holder.bindItem(context as Activity)
                else -> (holder as PlacesFilterItemViewHolder).bindItem(filtersList[position])
            }
        }

        override fun onLangSelected() {
            notifyDataSetChanged()
        }
    }

    class PlacesFilterHeaderViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        private val headerText = itemView.findViewById<TextView>(R.id.filter_header_title)!!

        fun bindItem(context: Context, filterHeader: String) {
            headerText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.primary_color))
            headerText.text = filterHeader
        }
    }
    class PlacesFilterFooterViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        fun bindItem(activity: Activity) {
            itemView.setOnClickListener {
                (activity as PlacesFilterActivity).addLanguageLauncher.launch(WikipediaLanguagesActivity.newIntent(itemView.context,
                    Constants.InvokeSource.PLACES))
            }
        }
    }
    class PlacesFilterItemViewHolder(itemView: View, val callback: Callback) : DefaultViewHolder<View>(itemView) {
        interface Callback {
            fun onLangSelected()
        }
        private val titleText = itemView.findViewById<TextView>(R.id.placesFilterTitle)!!
        private val langCodeText = itemView.findViewById<LangCodeView>(R.id.placesFilterLangCode)!!
        private val radio = itemView.findViewById<ImageView>(R.id.placesFilterRadio)!!

        fun bindItem(languageCode: String) {
            titleText.text = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(languageCode)
            langCodeText.setLangCode(languageCode)
            radio.isVisible = languageCode == Prefs.placesWikiCode
            itemView.setOnClickListener {
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
