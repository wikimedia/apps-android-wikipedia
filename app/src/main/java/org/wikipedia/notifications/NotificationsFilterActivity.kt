package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityNotificationsFiltersBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import org.wikipedia.util.StringUtil.csvToList
import org.wikipedia.views.DefaultViewHolder

class NotificationsFilterActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsFiltersBinding
    private var notificationsFilterAdapter = NotificationsFilterAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsFiltersBinding.inflate(layoutInflater)
        setUpRecyclerView()
        setContentView(binding.root)
    }

    private fun setUpRecyclerView() {
        binding.notificationsFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsFiltersRecyclerView.adapter = notificationsFilterAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_filter_notification, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_notifications_filter -> {
                notificationsFilterAdapter.selectAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class NotificationFilterItemHolder constructor(itemView: NotificationFilterItemView) :
        DefaultViewHolder<NotificationFilterItemView>(itemView) {
        fun bindItem(langCode: String?, title: String, selected: Boolean, imageRes: Int?) {
            view.setContents(langCode, title, selected, imageRes)
        }
    }

    class NotificationsFilterAdapter(val context: Context) :
        RecyclerView.Adapter<NotificationFilterItemHolder>(), NotificationFilterItemView.Callback {
        var app: WikipediaApp = WikipediaApp.getInstance()
        private var filteredWikisList = mutableListOf<String>()
        private var fullWikisList = mutableListOf<String>()

        init {
            filteredWikisList.clear()
            fullWikisList.clear()
            fullWikisList.addAll(app.language().appLanguageCodes)
            fullWikisList.add("commons")
            fullWikisList.add("wikidata")
            filteredWikisList.addAll(if (Prefs.notificationsFilterLanguageCodes == null) fullWikisList
            else csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty()))
        }

        fun selectAll() {
            Prefs.notificationsFilterLanguageCodes = StringUtil.listToCsv(fullWikisList)
            filteredWikisList.clear()
            filteredWikisList.addAll(csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty()))
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): NotificationFilterItemHolder {
            val notificationsFilterItemView = NotificationFilterItemView(context)
            notificationsFilterItemView.callback = this
            return NotificationFilterItemHolder(notificationsFilterItemView)
        }

        override fun getItemCount(): Int {
            return fullWikisList.size
        }

        override fun onBindViewHolder(holder: NotificationFilterItemHolder, position: Int) {
            val languageCode = fullWikisList[position]
            val showCheck = filteredWikisList.contains(languageCode)
            when (languageCode) {
                "commons" -> {
                    holder.bindItem(null, context.getString(R.string.wikimedia_commons), showCheck, R.drawable.ic_commons_logo)
                }
                "wikidata" -> {
                    holder.bindItem(null, context.getString(R.string.wikidata), showCheck, R.drawable.ic_wikidata_logo)
                }
                else -> {
                    holder.bindItem(languageCode, app.language().getAppLanguageCanonicalName(languageCode).orEmpty(), showCheck, null)
                }
            }
        }

        override fun onCheckedChanged(langCode: String) {
            if (filteredWikisList.contains(langCode)) {
                filteredWikisList.remove(langCode)
            } else {
                filteredWikisList.add(langCode)
            }
            Prefs.notificationsFilterLanguageCodes = StringUtil.listToCsv(filteredWikisList)
            notifyDataSetChanged()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsFilterActivity::class.java)
        }
    }
}
