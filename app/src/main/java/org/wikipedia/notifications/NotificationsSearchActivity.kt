package org.wikipedia.notifications

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.R.attr
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityNotificationsSearchBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.notifications.NotificationActivity.*
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationsSearchActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsSearchBinding
    private val notificationList = mutableListOf<Notification>()
    private val notificationContainerList = mutableListOf<NotificationListItemContainer>()
    private var currentContinueStr: String? = null
    private val disposables = CompositeDisposable()
    var currentSearchQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleIntent(intent)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.nav_tab_background_color))
        setSupportActionBar(binding.searchToolbar)
        supportActionBar?.title = ""
        binding.searchCabView.isIconified = false
        binding.searchCabView.setOnQueryTextListener(searchQueryListener)
        binding.searchCabView.setOnCloseListener(searchCloseListener)
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.addItemDecoration(DrawableItemDecoration(this, attr.list_separator_drawable))
        binding.notificationsRecyclerView.adapter = NotificationItemAdapter()
orContinueNotifications
    }

    private val searchCloseListener = SearchView.OnCloseListener {
        // May be hide sofy key?
        return@OnCloseListener false
    }

    private val searchQueryListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryText: String): Boolean {
                parent?.let { DeviceUtil.hideSoftKeyboard(it) }
                return true
            }

            override fun onQueryTextChange(queryText: String): Boolean {
                binding.searchCabView.setCloseButtonVisibility(queryText)
                updateNotificationsList(queryText)
                currentSearchQuery = queryText.trim()
                postprocessAndDisplay()
                return true
            }
        }

    private fun updateNotificationsList(queryText: String) {

    }

    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            // use the query to search your data somehow
        }
    }

    private inner class NotificationItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return notificationContainerList.size
        }

        override fun getItemViewType(position: Int): Int {
            return notificationContainerList[position].type
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return if (type == NotificationListItemContainer.ITEM_DATE_HEADER) {
                NotificationDateHolder(layoutInflater.inflate(R.layout.item_notification_date, parent, false))
            } else {
                NotificationItemHolderSwipeable(layoutInflater.inflate(R.layout.item_notification, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            when (holder) {
                is NotificationDateHolder -> holder.bindItem(notificationContainerList[pos].date)
                is NotificationItemHolderSwipeable -> holder.bindItem(notificationContainerList[pos])
            }

            // if we're at the bottom of the list, and we have a continuation string, then execute it.
            if (pos == notificationContainerList.size - 1 && !currentContinueStr.isNullOrEmpty()) {
                orContinueNotifications
            }
        }
    }

    private val orContinueNotifications: Unit
        get() {
            //binding.notificationsProgressBar.visibility = View.VISIBLE
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getAllNotifications("*", "read", currentContinueStr).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ response ->
                    onNotificationsComplete(response.query?.notifications!!.list!!, !currentContinueStr.isNullOrEmpty())
                    currentContinueStr = response.query?.notifications!!.continueStr
                }) { t -> setErrorState(t) })
        }
    private fun onNotificationsComplete(notifications: List<Notification>, fromContinuation: Boolean) {
        setSuccessState()
        if (!fromContinuation) {
            notificationList.clear()
            binding.notificationsRecyclerView.adapter = NotificationItemAdapter()
        }
        for (n in notifications) {
            if (notificationList.none { it.id == n.id }) {
                notificationList.add(n)
            }
        }
        postprocessAndDisplay()
    }

    private fun postprocessAndDisplay() {
        // Sort them by descending date...
        notificationList.sortWith { n1: Notification, n2: Notification -> n2.getTimestamp().compareTo(n1.getTimestamp()) }

        // Build the container list, and punctuate it by date granularity, while also applying the
        // current search query.
        notificationContainerList.clear()
        var millis = Long.MAX_VALUE
        for (n in notificationList) {
            if (!currentSearchQuery.isNullOrEmpty() && n.contents != null && !n.contents.header.contains(currentSearchQuery!!)) {
                continue
            }
            if (millis - n.getTimestamp().time > TimeUnit.DAYS.toMillis(1)) {
                notificationContainerList.add(NotificationListItemContainer(n.getTimestamp()))
                millis = n.getTimestamp().time
            }
            notificationContainerList.add(NotificationListItemContainer(n))
        }
        binding.notificationsRecyclerView.adapter!!.notifyDataSetChanged()
        if (notificationContainerList.isEmpty()) {
            Log.e("####","HERE EMMPTY")
           // binding.notificationsEmptyContainer.visibility = View.VISIBLE
        } else {
           // binding.notificationsEmptyContainer.visibility = View.GONE
        }
    }
    private fun setSuccessState() {
        //binding.notificationsProgressBar.visibility = View.GONE
       // binding.notificationsErrorView.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.VISIBLE
    }
    private fun setErrorState(t: Throwable) {
        L.e(t)
       /* binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.notificationsEmptyContainer.visibility = View.GONE
        binding.notificationsErrorView.setError(t)
        binding.notificationsErrorView.visibility = View.VISIBLE*/
    }

    @Suppress("LeakingThis")
    private open inner class NotificationItemHolder constructor(view: View) :
        RecyclerView.ViewHolder(view), View.OnClickListener, OnLongClickListener {
        private val titleView = view.findViewById<TextView>(R.id.notification_item_title)
        private val descriptionView =
            view.findViewById<TextView>(R.id.notification_item_description)
        private val secondaryActionHintView =
            view.findViewById<TextView>(R.id.notification_item_secondary_action_hint)
        private val tertiaryActionHintView =
            view.findViewById<TextView>(R.id.notification_item_tertiary_action_hint)
        private val wikiCodeView = view.findViewById<TextView>(R.id.notification_wiki_code)
        private val wikiCodeImageView =
            view.findViewById<AppCompatImageView>(R.id.notification_wiki_code_image)
        private val wikiCodeBackgroundView =
            view.findViewById<AppCompatImageView>(R.id.notification_wiki_code_background)
        private val imageContainerView =
            view.findViewById<View>(R.id.notification_item_image_container)
        private val imageBackgroundView =
            view.findViewById<AppCompatImageView>(R.id.notification_item_image_background)
        private val imageSelectedView =
            view.findViewById<View>(R.id.notification_item_selected_image)
        private val imageView = view.findViewById<AppCompatImageView>(R.id.notification_item_image)
        lateinit var container: NotificationListItemContainer

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            DeviceUtil.setContextClickAsLongClick(itemView)
        }

        fun bindItem(container: NotificationListItemContainer) {
            this.container = container
            val n = container.notification!!
            val notificationCategory = NotificationCategory.find(n.category)
            imageView.setImageResource(notificationCategory.iconResId)
            imageBackgroundView.drawable.setTint(ContextCompat.getColor(this@NotificationsSearchActivity, notificationCategory.iconColor))
            secondaryActionHintView.isVisible = false
            tertiaryActionHintView.isVisible = false
            n.contents?.let {
                titleView.text = StringUtil.fromHtml(it.header)
                if (it.body.trim().isNotEmpty()) {
                    descriptionView.text = StringUtil.fromHtml(it.body)
                    descriptionView.visibility = View.VISIBLE
                } else {
                    descriptionView.visibility = View.GONE
                }
                it.links?.secondary?.let { secondary ->
                    if (secondary.isNotEmpty()) {
                        secondaryActionHintView.text = secondary[0].label
                        secondaryActionHintView.visibility = View.VISIBLE
                        if (secondary.size > 1) {
                            tertiaryActionHintView.text = secondary[1].label
                            tertiaryActionHintView.visibility = View.VISIBLE
                        }
                    }
                }
            }
            val wikiCode = n.wiki
            when {
                wikiCode.contains("wikidata") -> {
                    wikiCodeView.visibility = View.GONE
                    wikiCodeBackgroundView.visibility = View.GONE
                    wikiCodeImageView.visibility = View.VISIBLE
                    wikiCodeImageView.setImageResource(R.drawable.ic_wikidata_logo)
                }
                wikiCode.contains("commons") -> {
                    wikiCodeView.visibility = View.GONE
                    wikiCodeBackgroundView.visibility = View.GONE
                    wikiCodeImageView.visibility = View.VISIBLE
                    wikiCodeImageView.setImageResource(R.drawable.ic_commons_logo)
                }
                else -> {
                    wikiCodeBackgroundView.visibility = View.VISIBLE
                    wikiCodeView.visibility = View.VISIBLE
                    wikiCodeImageView.visibility = View.GONE
                    val langCode = n.wiki.replace("wiki", "")
                    wikiCodeView.text = langCode
                    L10nUtil.setConditionalLayoutDirection(itemView, langCode)
                }
            }
            if (container.selected) {
                imageSelectedView.visibility = View.VISIBLE
                imageContainerView.visibility = View.INVISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationsSearchActivity, R.attr.multi_select_background_color))
            } else {
                imageSelectedView.visibility = View.INVISIBLE
                imageContainerView.visibility = View.VISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationsSearchActivity, R.attr.paper_color))
            }
        }

        override fun onClick(v: View) {
        }

        override fun onLongClick(v: View): Boolean {
            return true
        }
    }

    private inner class NotificationDateHolder constructor(view: View) :
        RecyclerView.ViewHolder(view) {
        private val dateView: TextView = view.findViewById(R.id.notification_date_text)
        fun bindItem(date: Date?) {
            dateView.text = DateUtil.getFeedCardDateString(date!!)
        }
    }

    private inner class NotificationItemHolderSwipeable constructor(v: View) :
        NotificationItemHolder(v), SwipeableItemTouchHelperCallback.Callback {
        override fun onSwipe() {
        }
    }

    private class NotificationListItemContainer {
        val type: Int
        var notification: Notification? = null
        var date: Date? = null
        var selected = false

        constructor(date: Date) {
            this.date = date
            type = ITEM_DATE_HEADER
        }

        constructor(notification: Notification) {
            this.notification = notification
            type = ITEM_NOTIFICATION
        }

        companion object {
            const val ITEM_DATE_HEADER = 0
            const val ITEM_NOTIFICATION = 1
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsSearchActivity::class.java)
        }
    }
}
