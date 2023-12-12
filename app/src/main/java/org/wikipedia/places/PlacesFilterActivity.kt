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
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.ViewUtil

class PlacesFilterActivity : BaseActivity() {
    private lateinit var binding: ActivityPlacesFiltersBinding
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
        runOnUiThread { binding.placesFiltersRecyclerView.adapter?.notifyDataSetChanged() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlacesFiltersBinding.inflate(layoutInflater)
        setResult(RESULT_OK)
        setUpRecyclerView()
        setContentView(binding.root)
    }

    private fun setUpRecyclerView() {
        val list = mutableListOf<String>()
        list.add(HEADER)
        list.addAll(appLanguageCodes)
        list.add(FOOTER)
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
                    context, getString(R.string.places_filter_header)
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
        private val langCodeText = itemView.findViewById<TextView>(R.id.placesFilterLangCode)!!
        private val radio = itemView.findViewById<ImageView>(R.id.placesFilterRadio)!!

        fun bindItem(languageCode: String) {
            titleText.text = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(languageCode)
            langCodeText.text = languageCode
            radio.isVisible = languageCode == Prefs.placesWikiCode
            ViewUtil.formatLangButton(langCodeText, languageCode,
                SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
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
        fun newIntent(context: Context): Intent {
            return Intent(context, PlacesFilterActivity::class.java)
        }
    }
}
