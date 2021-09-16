package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityNotificationsFiltersBinding
import org.wikipedia.notifications.NotificationsFilterItemView.Callback
import org.wikipedia.settings.Prefs
import org.wikipedia.util.StringUtil
import org.wikipedia.util.StringUtil.csvToList
import org.wikipedia.views.DefaultViewHolder

class NotificationsFiltersActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsFiltersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsFiltersBinding.inflate(layoutInflater)
        setUpFiltersRecyclerView()
        setContentView(binding.root)
    }

    private fun setUpFiltersRecyclerView() {
        binding.notificationsFiltersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsFiltersRecyclerView.adapter = NotificationsFiltersAdapter(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_filter_notification, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_notifications_filter -> {
                // Todo: implement
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class NotificationFilterItemHolder constructor(itemView: NotificationsFilterItemView) :
        DefaultViewHolder<NotificationsFilterItemView>(itemView) {
        fun bindItem(langCode: String?, title: String, selected: Boolean, imageRes: Int?) {
            view.setContents(langCode, title, selected, imageRes)
        }
    }

    class NotificationsFiltersAdapter(val context: Context) :
        RecyclerView.Adapter<NotificationFilterItemHolder>(), Callback {
        var app: WikipediaApp = WikipediaApp.getInstance()
        var appLanguageCodes: MutableList<String> = app.language().appLanguageCodes
        private var fullWikisList = mutableListOf<String>()

        init {
            fullWikisList.addAll(appLanguageCodes)
            fullWikisList.add("commons")
            fullWikisList.add("wikidata")
            filteredWikisList.addAll(if (Prefs.getNotificationsFilterLanguageCodes() == null) fullWikisList
            else csvToList(StringUtils.defaultString(Prefs.getNotificationsFilterLanguageCodes())))
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): NotificationFilterItemHolder {
            val notificationsFilterItemView = NotificationsFilterItemView(context)
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
                "commons" -> { holder.bindItem(null, context.getString(R.string.wikimedia_commons), showCheck, R.drawable.ic_commons_logo) }
                "wikidata" -> { holder.bindItem(null, context.getString(R.string.wikidata), showCheck, R.drawable.ic_wikidata_logo) }
                else -> { holder.bindItem(languageCode, app.language().getAppLanguageCanonicalName(languageCode).orEmpty(), showCheck, null) }
            }
        }

        override fun onCheckedChanged(langCode: String) {
            if (filteredWikisList.contains(langCode)) {
                filteredWikisList.remove(langCode)
            } else {
                filteredWikisList.add(langCode)
            }
            Prefs.setNotificationsFilterLanguageCodes(StringUtil.listToCsv(filteredWikisList))
            notifyDataSetChanged()
        }
    }

    companion object {
        private var filteredWikisList = mutableListOf<String>()

        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsFiltersActivity::class.java)
        }
    }
}
