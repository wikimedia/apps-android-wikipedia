package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.NotificationFunnel
import org.wikipedia.databinding.ActivityNotificationsBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.NotificationSettingsActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil.getFeedCardDateString
import org.wikipedia.util.DeviceUtil.setContextClickAsLongClick
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.MultiSelectActionModeCallback
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationActivity : BaseActivity(), NotificationItemActionsDialog.Callback {
    private lateinit var binding: ActivityNotificationsBinding

    private val notificationList = mutableListOf<Notification>()
    private val notificationContainerList = mutableListOf<NotificationListItemContainer>()
    private val disposables = CompositeDisposable()
    private val dbNameMap = mutableMapOf<String, WikiSite>()
    private var currentContinueStr: String? = null
    private var actionMode: ActionMode? = null
    private val multiSelectActionModeCallback = MultiSelectCallback()
    private val searchActionModeCallback = SearchCallback()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var displayArchived = false
    var currentSearchQuery: String? = null

    override val isShowingArchived get() = displayArchived

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground))
        binding.notificationsErrorView.retryClickListener = View.OnClickListener { beginUpdateList() }
        binding.notificationsErrorView.backClickListener = View.OnClickListener { onBackPressed() }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable))

        val touchCallback = SwipeableItemTouchHelperCallback(this,
                ResourceUtil.getThemedAttributeId(this, R.attr.chart_shade5),
                R.drawable.ic_archive_white_24dp,
                ResourceUtil.getThemedAttributeId(this, R.attr.secondary_text_color))

        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.notificationsRecyclerView)

        binding.notificationsRefreshView.setOnRefreshListener {
            binding.notificationsRefreshView.isRefreshing = false
            beginUpdateList()
        }

        binding.notificationsViewArchivedButton.setOnClickListener { onViewArchivedClick() }

        beginUpdateList()
        NotificationSettingsActivity.promptEnablePollDialog(this)
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val itemArchived = menu.findItem(R.id.menu_notifications_view_archived)
        val itemUnread = menu.findItem(R.id.menu_notifications_view_unread)
        itemArchived.isVisible = !displayArchived
        itemUnread.isVisible = displayArchived
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_notifications_view_archived -> {
                onViewArchivedClick()
                true
            }
            R.id.menu_notifications_view_unread -> {
                displayArchived = false
                beginUpdateList()
                true
            }
            R.id.menu_notifications_prefs -> {
                startActivity(NotificationSettingsActivity.newIntent(this))
                true
            }
            R.id.menu_notifications_search -> {
                startSupportActionMode(searchActionModeCallback)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (displayArchived) {
            displayArchived = false
            beginUpdateList()
            return
        }
        super.onBackPressed()
    }

    private fun onViewArchivedClick() {
        displayArchived = true
        beginUpdateList()
    }

    private fun beginUpdateList() {
        binding.notificationsErrorView.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.notificationsEmptyContainer.visibility = View.GONE
        binding.notificationsProgressBar.visibility = View.VISIBLE
        supportActionBar?.setTitle(if (displayArchived) R.string.notifications_activity_title_archived else R.string.notifications_activity_title)
        currentContinueStr = null
        disposables.clear()

        // if we're not checking for unread notifications, then short-circuit straight to fetching them.
        if (displayArchived) {
            orContinueNotifications
            return
        }
        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).unreadNotificationWikis
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val wikiMap = response.query()!!.unreadNotificationWikis()
                    dbNameMap.clear()
                    for (key in wikiMap!!.keys) {
                        if (wikiMap[key]!!.source != null) {
                            dbNameMap[key] = WikiSite(wikiMap[key]!!.source!!.base)
                        }
                    }
                    orContinueNotifications
                }) { t -> setErrorState(t) })
    }

    private val orContinueNotifications: Unit
        get() {
            binding.notificationsProgressBar.visibility = View.VISIBLE
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getAllNotifications("*", if (displayArchived) "read" else "!read", currentContinueStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        onNotificationsComplete(response.query()!!.notifications()!!.list()!!, !currentContinueStr.isNullOrEmpty())
                        currentContinueStr = response.query()!!.notifications()!!.getContinue()
                    }) { t -> setErrorState(t) })
        }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.notificationsEmptyContainer.visibility = View.GONE
        binding.notificationsErrorView.setError(t)
        binding.notificationsErrorView.visibility = View.VISIBLE
    }

    private fun setSuccessState() {
        binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsErrorView.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.VISIBLE
    }

    private fun onNotificationsComplete(notifications: List<Notification>, fromContinuation: Boolean) {
        setSuccessState()
        if (!fromContinuation) {
            notificationList.clear()
            binding.notificationsRecyclerView.adapter = NotificationItemAdapter()
        }
        for (n in notifications) {
            if (notificationList.none { it.id() == n.id() }) {
                notificationList.add(n)
            }
        }
        postprocessAndDisplay()
    }

    private fun postprocessAndDisplay() {
        // Sort them by descending date...
        notificationList.sortWith { n1: Notification, n2: Notification -> n2.timestamp.compareTo(n1.timestamp) }

        // Build the container list, and punctuate it by date granularity, while also applying the
        // current search query.
        notificationContainerList.clear()
        var millis = Long.MAX_VALUE
        for (n in notificationList) {

            // TODO: remove this condition when the time is right.
            if (n.category().startsWith(Notification.CATEGORY_SYSTEM) && Prefs.notificationWelcomeEnabled() ||
                    n.category() == Notification.CATEGORY_EDIT_THANK && Prefs.notificationThanksEnabled() ||
                    n.category() == Notification.CATEGORY_THANK_YOU_EDIT && Prefs.notificationMilestoneEnabled() ||
                    n.category() == Notification.CATEGORY_REVERTED && Prefs.notificationRevertEnabled() ||
                    n.category() == Notification.CATEGORY_EDIT_USER_TALK && Prefs.notificationUserTalkEnabled() ||
                    n.category() == Notification.CATEGORY_LOGIN_FAIL && Prefs.notificationLoginFailEnabled() ||
                    n.category().startsWith(Notification.CATEGORY_MENTION) && Prefs.notificationMentionEnabled() ||
                    Prefs.showAllNotifications()) {
                if (!currentSearchQuery.isNullOrEmpty() && n.contents != null && !n.contents!!.header.contains(currentSearchQuery!!)) {
                    continue
                }
                if (millis - n.timestamp.time > TimeUnit.DAYS.toMillis(1)) {
                    notificationContainerList.add(NotificationListItemContainer(n.timestamp))
                    millis = n.timestamp.time
                }
                notificationContainerList.add(NotificationListItemContainer(n))
            }
        }
        binding.notificationsRecyclerView.adapter!!.notifyDataSetChanged()
        if (notificationContainerList.isEmpty()) {
            binding.notificationsEmptyContainer.visibility = View.VISIBLE
            binding.notificationsViewArchivedButton.visibility = if (displayArchived) View.GONE else View.VISIBLE
        } else {
            binding.notificationsEmptyContainer.visibility = View.GONE
        }
    }

    private fun deleteItems(items: List<NotificationListItemContainer>, markUnread: Boolean) {
        val notificationsPerWiki: MutableMap<WikiSite, MutableList<Notification>> = HashMap()
        val selectionKey = if (items.size > 1) Random().nextLong() else null
        for (item in items) {
            val wiki = if (dbNameMap.containsKey(item.notification!!.wiki())) dbNameMap[item.notification!!.wiki()]!! else WikipediaApp.getInstance().wikiSite
            if (!notificationsPerWiki.containsKey(wiki)) {
                notificationsPerWiki[wiki] = ArrayList()
            }
            notificationsPerWiki[wiki]!!.add(item.notification!!)
            if (markUnread && !displayArchived) {
                notificationList.add(item.notification!!)
            } else {
                notificationList.remove(item.notification)
                NotificationFunnel(WikipediaApp.getInstance(), item.notification).logMarkRead(selectionKey)
            }
        }
        for (wiki in notificationsPerWiki.keys) {
            if (markUnread) {
                NotificationPollBroadcastReceiver.markRead(wiki, notificationsPerWiki[wiki]!!, true)
            } else {
                NotificationPollBroadcastReceiver.markRead(wiki, notificationsPerWiki[wiki]!!, false)
                showDeleteItemsUndoSnackbar(items)
            }
        }
        postprocessAndDisplay()
    }

    private fun showDeleteItemsUndoSnackbar(items: List<NotificationListItemContainer>) {
        val snackbar = FeedbackUtil.makeSnackbar(this, resources.getQuantityString(R.plurals.notification_archive_message, items.size, items.size), FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.notification_archive_undo) { deleteItems(items, true) }
        snackbar.show()
    }

    private fun finishActionMode() {
        actionMode?.finish()
    }

    private fun beginMultiSelect() {
        if (SearchActionModeCallback.`is`(actionMode)) {
            finishActionMode()
        }
        if (!MultiSelectActionModeCallback.isTagType(actionMode)) {
            startSupportActionMode(multiSelectActionModeCallback)
        }
    }

    private fun toggleSelectItem(container: NotificationListItemContainer) {
        container.selected = !container.selected
        val selectedCount = selectedItemCount
        if (selectedCount == 0) {
            finishActionMode()
        } else if (actionMode != null) {
            actionMode!!.title = resources.getQuantityString(R.plurals.multi_items_selected, selectedCount, selectedCount)
        }
        binding.notificationsRecyclerView.adapter?.notifyDataSetChanged()
    }

    private val selectedItemCount get() = notificationContainerList.count { it.selected }

    private fun unselectAllItems() {
        for (item in notificationContainerList) {
            item.selected = false
        }
        binding.notificationsRecyclerView.adapter?.notifyDataSetChanged()
    }

    private val selectedItems: List<NotificationListItemContainer>
        get() {
            val result: MutableList<NotificationListItemContainer> = ArrayList()
            for (item in notificationContainerList) {
                if (item.selected) {
                    result.add(item)
                }
            }
            return result
        }

    override fun onArchive(notification: Notification) {
        bottomSheetPresenter.dismiss(supportFragmentManager)
        for (c in notificationContainerList) {
            if (c.notification != null && c.notification!!.key() == notification.key()) {
                deleteItems(listOf(c), displayArchived)
                break
            }
        }
    }

    override fun onActionPageTitle(pageTitle: PageTitle) {
        startActivity(PageActivity.newIntentForCurrentTab(this,
                HistoryEntry(pageTitle, HistoryEntry.SOURCE_NOTIFICATION), pageTitle))
    }

    @Suppress("LeakingThis")
    private open inner class NotificationItemHolder constructor(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, OnLongClickListener {
        lateinit var container: NotificationListItemContainer
        private val titleView: TextView
        private val descriptionView: TextView
        private val wikiCodeView: TextView
        private val secondaryActionHintView: TextView
        private val tertiaryActionHintView: TextView
        private val wikiCodeBackgroundView: AppCompatImageView
        private val wikiCodeImageView: AppCompatImageView
        private val imageContainerView: View
        private val imageSelectedView: View
        private val imageView: AppCompatImageView
        private val imageBackgroundView: AppCompatImageView

        fun bindItem(container: NotificationListItemContainer) {
            this.container = container
            val n = container.notification!!
            var iconResId = R.drawable.ic_speech_bubbles
            var iconBackColor = R.color.accent50
            val s = n.category()
            when {
                Notification.CATEGORY_EDIT_USER_TALK == s -> {
                    iconResId = R.drawable.ic_edit_user_talk
                    iconBackColor = R.color.accent50
                }
                Notification.CATEGORY_REVERTED == s -> {
                    iconResId = R.drawable.ic_revert
                    iconBackColor = R.color.base20
                }
                Notification.CATEGORY_EDIT_THANK == s -> {
                    iconResId = R.drawable.ic_user_talk
                    iconBackColor = R.color.green50
                }
                Notification.CATEGORY_THANK_YOU_EDIT == s -> {
                    iconResId = R.drawable.ic_edit_progressive
                    iconBackColor = R.color.accent50
                }
                s.startsWith(Notification.CATEGORY_MENTION) -> {
                    iconResId = R.drawable.ic_mention
                    iconBackColor = R.color.accent50
                }
                Notification.CATEGORY_LOGIN_FAIL == s -> {
                    iconResId = R.drawable.ic_user_avatar
                    iconBackColor = R.color.base0
                }
            }
            imageView.setImageResource(iconResId)
            DrawableCompat.setTint(imageBackgroundView.drawable,
                    ContextCompat.getColor(this@NotificationActivity, iconBackColor))
            n.contents?.let {
                titleView.text = StringUtil.fromHtml(it.header)
                if (it.body.trim().isNotEmpty()) {
                    descriptionView.text = StringUtil.fromHtml(it.body)
                    descriptionView.visibility = View.VISIBLE
                } else {
                    descriptionView.visibility = View.GONE
                }
                it.links?.secondary?.let { secondary ->
                    if (secondary.size > 0) {
                        secondaryActionHintView.text = secondary[0].label
                        secondaryActionHintView.visibility = View.VISIBLE
                        if (secondary.size > 1) {
                            tertiaryActionHintView.text = secondary[1].label
                            tertiaryActionHintView.visibility = View.VISIBLE
                        }
                    }
                }
            }
            val wikiCode = n.wiki()
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
                    wikiCodeView.text = n.wiki().replace("wiki", "")
                }
            }
            if (container.selected) {
                imageSelectedView.visibility = View.VISIBLE
                imageContainerView.visibility = View.INVISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.multi_select_background_color))
            } else {
                imageSelectedView.visibility = View.INVISIBLE
                imageContainerView.visibility = View.VISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.paper_color))
            }
        }

        override fun onClick(v: View) {
            if (MultiSelectActionModeCallback.isTagType(actionMode)) {
                toggleSelectItem(container)
            } else {
                bottomSheetPresenter.show(supportFragmentManager,
                        NotificationItemActionsDialog.newInstance(container.notification!!))
            }
        }

        override fun onLongClick(v: View): Boolean {
            beginMultiSelect()
            toggleSelectItem(container)
            return true
        }

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            titleView = view.findViewById(R.id.notification_item_title)
            descriptionView = view.findViewById(R.id.notification_item_description)
            secondaryActionHintView = view.findViewById(R.id.notification_item_secondary_action_hint)
            tertiaryActionHintView = view.findViewById(R.id.notification_item_tertiary_action_hint)
            wikiCodeView = view.findViewById(R.id.notification_wiki_code)
            wikiCodeImageView = view.findViewById(R.id.notification_wiki_code_image)
            wikiCodeBackgroundView = view.findViewById(R.id.notification_wiki_code_background)
            imageContainerView = view.findViewById(R.id.notification_item_image_container)
            imageBackgroundView = view.findViewById(R.id.notification_item_image_background)
            imageSelectedView = view.findViewById(R.id.notification_item_selected_image)
            imageView = view.findViewById(R.id.notification_item_image)
            setContextClickAsLongClick(itemView)
        }
    }

    private inner class NotificationItemHolderSwipeable constructor(v: View) : NotificationItemHolder(v), SwipeableItemTouchHelperCallback.Callback {
        override fun onSwipe() {
            deleteItems(listOf(container), false)
        }
    }

    private inner class NotificationDateHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        private val dateView: TextView = view.findViewById(R.id.notification_date_text)
        fun bindItem(date: Date?) {
            dateView.text = getFeedCardDateString(date!!)
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
            if (type == NotificationListItemContainer.ITEM_DATE_HEADER) {
                return NotificationDateHolder(layoutInflater.inflate(R.layout.item_notification_date, parent, false))
            }
            return if (displayArchived) {
                NotificationItemHolder(layoutInflater.inflate(R.layout.item_notification, parent, false))
            } else {
                NotificationItemHolderSwipeable(layoutInflater.inflate(R.layout.item_notification, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            when (holder) {
                is NotificationDateHolder -> holder.bindItem(notificationContainerList[pos].date)
                is NotificationItemHolderSwipeable -> holder.bindItem(notificationContainerList[pos])
                is NotificationItemHolder -> holder.bindItem(notificationContainerList[pos])
            }

            // if we're at the bottom of the list, and we have a continuation string, then execute it.
            if (pos == notificationContainerList.size - 1 && !currentContinueStr.isNullOrEmpty()) {
                orContinueNotifications
            }
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            currentSearchQuery = s.trim()
            postprocessAndDisplay()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            currentSearchQuery = null
            postprocessAndDisplay()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.notifications_search)
        }

        override fun getParentContext(): Context {
            return this@NotificationActivity
        }
    }

    private inner class MultiSelectCallback : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.menuInflater.inflate(R.menu.menu_action_mode_notifications, menu)
            menu.findItem(R.id.menu_delete_selected).isVisible = !displayArchived
            menu.findItem(R.id.menu_unarchive_selected).isVisible = displayArchived
            actionMode = mode
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_delete_selected, R.id.menu_unarchive_selected -> {
                    onDeleteSelected()
                    finishActionMode()
                    return true
                }
            }
            return false
        }

        override fun onDeleteSelected() {
            deleteItems(selectedItems, displayArchived)
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            unselectAllItems()
            actionMode = null
            super.onDestroyActionMode(mode)
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
            return Intent(context, NotificationActivity::class.java)
        }
    }
}
