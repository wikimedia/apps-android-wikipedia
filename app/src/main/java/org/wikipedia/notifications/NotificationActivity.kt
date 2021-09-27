package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.NotificationsABCTestFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.databinding.ActivityNotificationsBinding
import org.wikipedia.databinding.ItemNotificationBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.NotificationSettingsActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.DeviceUtil.setContextClickAsLongClick
import org.wikipedia.util.log.L
import org.wikipedia.views.*
import java.util.*

class NotificationActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    private val notificationList = mutableListOf<Notification>()
    private val notificationContainerList = mutableListOf<NotificationListItemContainer>()
    private val disposables = CompositeDisposable()
    private val dbNameMap = mutableMapOf<String, WikiSite>()
    private var currentContinueStr: String? = null
    private var actionMode: ActionMode? = null
    private val multiSelectActionModeCallback = MultiSelectCallback()
    private val searchActionModeCallback = SearchCallback()
    private var linkHandler = NotificationLinkHandler(this)
    var currentSearchQuery: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground))
        binding.notificationsErrorView.retryClickListener = View.OnClickListener { beginUpdateList() }
        binding.notificationsErrorView.backClickListener = View.OnClickListener { onBackPressed() }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, skipSearchBar = true))

        val touchCallback = SwipeableItemTouchHelperCallback(this,
                ResourceUtil.getThemedAttributeId(this, R.attr.chart_shade5),
                R.drawable.ic_archive_white_24dp,
                ResourceUtil.getThemedAttributeId(this, R.attr.secondary_text_color))

        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.notificationsRecyclerView)

        binding.notificationsRefreshView.setOnRefreshListener {
            binding.notificationsRefreshView.isRefreshing = false
            actionMode?.finish()
            beginUpdateList()
        }

        binding.notificationTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                postprocessAndDisplay()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        Prefs.notificationUnreadCount = 0
        NotificationsABCTestFunnel().logSelect()

        beginUpdateList()
    }

    override fun onResume() {
        super.onResume()
        beginUpdateList()
        actionMode?.let {
            if (SearchActionModeCallback.`is`(it)) {
                searchActionModeCallback.refreshProvider()
            }
        }
    }

    public override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_notifications_mark_all_as_read -> {
                if (notificationContainerList.isNotEmpty()) {
                    markReadItems(notificationContainerList
                        .filterNot { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }
                        .filter { it.notification?.isUnread == true }, false)
                }
                true
            }
            R.id.menu_notifications_prefs -> {
                startActivity(NotificationSettingsActivity.newIntent(this))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun beginUpdateList() {
        binding.notificationsErrorView.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.notificationsEmptyContainer.visibility = View.GONE
        binding.notificationsSearchEmptyContainer.visibility = View.GONE
        binding.notificationsProgressBar.visibility = View.VISIBLE
        binding.notificationTabLayout.visibility = View.GONE
        supportActionBar?.setTitle(R.string.notifications_activity_title)
        currentContinueStr = null
        disposables.clear()

        disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).unreadNotificationWikis
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val wikiMap = response.query?.unreadNotificationWikis
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
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getAllNotifications(delimitedFilteredWikiList(), "read|!read", currentContinueStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        onNotificationsComplete(response.query?.notifications!!.list!!, !currentContinueStr.isNullOrEmpty())
                        currentContinueStr = response.query?.notifications!!.continueStr
                    }) { t -> setErrorState(t) })
        }

    private fun delimitedFilteredWikiList(): String {
        val filteredWikiList = mutableListOf<String>()
        if (Prefs.notificationsFilterLanguageCodes == null) {
            WikipediaApp.getInstance().language().appLanguageCodes.forEach {
                val defaultLangCode = WikipediaApp.getInstance().language().getDefaultLanguageCode(it) ?: it
                filteredWikiList.add("${defaultLangCode.replace("-", "_")}wiki")
            }
            filteredWikiList.add("commonswiki")
            filteredWikiList.add("wikidatawiki")
        } else {
            val wikiTypeList = StringUtil.csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty())
            wikiTypeList.filter { WikipediaApp.getInstance().language().appLanguageCodes.contains(it) }.forEach { langCode ->
                val defaultLangCode = WikipediaApp.getInstance().language().getDefaultLanguageCode(langCode) ?: langCode
                filteredWikiList.add("${defaultLangCode.replace("-", "_")}wiki")
            }
            wikiTypeList.filter { it == "commons" || it == "wikidata" }.forEach { langCode ->
                filteredWikiList.add("${langCode}wiki")
            }
        }
        return filteredWikiList.joinToString("|")
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.notificationsEmptyContainer.visibility = View.GONE
        binding.notificationsSearchEmptyContainer.visibility = View.GONE
        binding.notificationsErrorView.setError(t)
        binding.notificationsErrorView.visibility = View.VISIBLE
    }

    private fun setSuccessState() {
        binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsErrorView.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.VISIBLE
        binding.notificationTabLayout.visibility = View.VISIBLE
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

        val allTab = binding.notificationTabLayout.getTabAt(0)!!
        val allUnreadCount = notificationList.count { it.isUnread }
        if (allUnreadCount > 0) {
            allTab.text = getString(R.string.notifications_tab_filter_all) + " " +
                    getString(R.string.notifications_tab_filter_unread, allUnreadCount.toString())
        } else {
            allTab.text = getString(R.string.notifications_tab_filter_all)
        }

        val mentionsTab = binding.notificationTabLayout.getTabAt(1)!!
        val mentionsUnreadCount = notificationList.filter { NotificationCategory.isMentionsGroup(it.category) }.count { it.isUnread }
        if (mentionsUnreadCount > 0) {
            mentionsTab.text = getString(R.string.notifications_tab_filter_mentions) + " " +
                    getString(R.string.notifications_tab_filter_unread, mentionsUnreadCount.toString())
        } else {
            mentionsTab.text = getString(R.string.notifications_tab_filter_mentions)
        }

        // Build the container list, and punctuate it by date granularity, while also applying the
        // current search query.
        notificationContainerList.clear()
        if (actionMode == null) notificationContainerList.add(NotificationListItemContainer()) // search bar
        binding.notificationTabLayout.visibility = if (actionMode != null) View.GONE else View.VISIBLE

        val selectedFilterTab = binding.notificationTabLayout.selectedTabPosition
        val filteredList = notificationList.filter { selectedFilterTab == 0 || (selectedFilterTab == 1 && NotificationCategory.isMentionsGroup(it.category)) }

        for (n in filteredList) {
            if (!currentSearchQuery.isNullOrEmpty() && n.contents != null && !n.contents.header.contains(currentSearchQuery!!)) {
                continue
            }
            val filterList = mutableListOf<String>()
            filterList.addAll(StringUtil.csvToList(Prefs.notificationsFilterLanguageCodes.orEmpty()).filter { NotificationCategory.isFiltersGroup(it) })
            if (filterList.contains(n.category)) notificationContainerList.add(NotificationListItemContainer(n))
        }
        binding.notificationsRecyclerView.adapter!!.notifyDataSetChanged()
        if (notificationContainerList.isEmpty()) {
            binding.notificationsEmptyContainer.visibility = if (actionMode == null) View.VISIBLE else View.GONE
            binding.notificationsSearchEmptyContainer.visibility = if (actionMode != null && enabledFiltersCount() != 0) View.VISIBLE else View.GONE
            binding.notificationsSearchEmptyText.visibility = if (actionMode != null && enabledFiltersCount() == 0) View.VISIBLE else View.GONE
            binding.notificationsEmptySearchMessage.setText(getSpannedEmptySearchMessage(), TextView.BufferType.SPANNABLE)
        } else {
            binding.notificationsEmptyContainer.visibility = View.GONE
            binding.notificationsSearchEmptyContainer.visibility = View.GONE
        }
    }

    private fun enabledFiltersCount(): Int {
        val fullWikiAndTypeListSize = NotificationsFilterActivity.allWikisList().size + NotificationsFilterActivity.allTypesIdList().size
        val filtersSize = Prefs.notificationsFilterLanguageCodes.orEmpty().split(",").filter { it.isNotEmpty() }.size
        return fullWikiAndTypeListSize - filtersSize
    }

    private fun getSpannedEmptySearchMessage(): Spannable {
        val filtersStr = resources.getQuantityString(R.plurals.notifications_number_of_filters, enabledFiltersCount(), enabledFiltersCount())
        val emptySearchMessage = getString(R.string.notifications_empty_search_message, filtersStr)
        val spannable = SpannableString(emptySearchMessage)
        val prefixStringLength = 13
        spannable.setSpan(ForegroundColorSpan(ResourceUtil.getThemedColor(this, R.attr.colorAccent)), prefixStringLength, prefixStringLength + filtersStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private fun markReadItems(items: List<NotificationListItemContainer>, markUnread: Boolean, fromUndo: Boolean = false) {
        val notificationsPerWiki: MutableMap<WikiSite, MutableList<Notification>> = HashMap()
        val selectionKey = if (items.size > 1) Random().nextLong() else null
        for (item in items) {
            val notification = item.notification!!
            val wiki = dbNameMap.getOrElse(notification.wiki) {
                when (notification.wiki) {
                    "commonswiki" -> WikiSite(Service.COMMONS_URL)
                    "wikidatawiki" -> WikiSite(Service.WIKIDATA_URL)
                    else -> {
                        val langCode = notification.wiki.replace("wiki", "").replace("_", "-")
                        WikiSite.forLanguageCode(WikipediaApp.getInstance().language().getDefaultLanguageCode(langCode) ?: langCode)
                    }
                }
            }
            notificationsPerWiki.getOrPut(wiki) { ArrayList() }.add(notification)
            if (!markUnread) {
                NotificationInteractionFunnel(WikipediaApp.getInstance(), notification).logMarkRead(selectionKey)
                NotificationInteractionEvent.logMarkRead(notification, selectionKey)
            }
        }

        for (wiki in notificationsPerWiki.keys) {
            NotificationPollBroadcastReceiver.markRead(wiki, notificationsPerWiki[wiki]!!, markUnread)
        }

        if (!fromUndo) {
            showMarkReadItemsUndoSnackbar(items, markUnread)
        }

        // manually mark items in read state
        notificationList.filter { n -> items.map { container -> container.notification?.id }
            .firstOrNull { it == n.id } != null }.map { it.read = if (markUnread) null else Date().toString() }
        postprocessAndDisplay()
    }

    private fun showMarkReadItemsUndoSnackbar(items: List<NotificationListItemContainer>, markUnread: Boolean) {
        val snackbarStringRes = if (markUnread) R.string.notifications_mark_all_as_unread_message else R.string.notifications_mark_all_as_read_message
        val snackbar = FeedbackUtil.makeSnackbar(this, getString(snackbarStringRes, items.size), FeedbackUtil.LENGTH_DEFAULT)
        snackbar.setAction(R.string.notification_archive_undo) { markReadItems(items, !markUnread, true) }
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
            actionMode?.title = selectedCount.toString()
        }
        binding.notificationsRecyclerView.adapter?.notifyDataSetChanged()
    }

    private val selectedItemCount get() = notificationContainerList.count { it.selected }

    private val selectedItems get() = notificationContainerList.filter { it.selected }

    @Suppress("LeakingThis")
    private open inner class NotificationItemHolder constructor(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener, SwipeableItemTouchHelperCallback.Callback {

        lateinit var container: NotificationListItemContainer

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            setContextClickAsLongClick(itemView)
        }

        fun bindItem(container: NotificationListItemContainer) {
            this.container = container
            val n = container.notification!!
            val notificationCategory = NotificationCategory.find(n.category)
            val notificationColor = ContextCompat.getColor(this@NotificationActivity, notificationCategory.iconColor)
            binding.notificationItemImage.setImageResource(notificationCategory.iconResId)
            binding.notificationItemImage.setColorFilter(notificationColor)
            n.contents?.let {
                binding.notificationSubtitle.text = StringUtil.fromHtml(it.header)
                StringUtil.highlightAndBoldenText(binding.notificationSubtitle, currentSearchQuery, true, Color.YELLOW)
                if (it.body.trim().isNotEmpty() && it.body.trim().isNotBlank()) {
                    binding.notificationDescription.text = StringUtil.fromHtml(it.body)
                    binding.notificationDescription.visibility = View.VISIBLE
                } else {
                    binding.notificationDescription.visibility = View.GONE
                }
                it.links?.secondary?.firstOrNull()?.let { link ->
                    binding.notificationTitle.text = link.label
                } ?: run {
                    binding.notificationTitle.text = getString(notificationCategory.title)
                }
            }

            // TODO: use better diff date method
            binding.notificationTime.text = DateUtils.getRelativeTimeSpanString(n.getTimestamp().time, System.currentTimeMillis(), 0L)

            binding.notificationItemReadDot.isVisible = n.isUnread
            binding.notificationItemReadDot.setColorFilter(notificationColor)
            binding.notificationTitle.typeface = if (n.isUnread) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            binding.notificationTitle.setTextColor(notificationColor)
            binding.notificationSubtitle.typeface = if (n.isUnread) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            val wikiCode = n.wiki
            val langCode = wikiCode.replace("wiki", "")
            L10nUtil.setConditionalLayoutDirection(itemView, langCode)

            n.title?.let { title ->
                binding.notificationSource.text = title.full
                n.contents?.links?.getPrimary()?.url?.run {
                    binding.notificationSourceExternalIcon.isVisible = !UriUtil.isAppSupportedLink(Uri.parse(this))
                }
                when {
                    wikiCode.contains("wikidata") -> {
                        binding.notificationWikiCode.visibility = View.GONE
                        binding.notificationWikiCodeBackground.visibility = View.GONE
                        binding.notificationWikiCodeImage.visibility = View.VISIBLE
                        binding.notificationWikiCodeImage.setImageResource(R.drawable.ic_wikidata_logo)
                    }
                    wikiCode.contains("commons") -> {
                        binding.notificationWikiCode.visibility = View.GONE
                        binding.notificationWikiCodeBackground.visibility = View.GONE
                        binding.notificationWikiCodeImage.visibility = View.VISIBLE
                        binding.notificationWikiCodeImage.setImageResource(R.drawable.ic_commons_logo)
                    }
                    else -> {
                        binding.notificationWikiCodeBackground.visibility = View.VISIBLE
                        binding.notificationWikiCode.visibility = View.VISIBLE
                        binding.notificationWikiCodeImage.visibility = View.GONE
                        binding.notificationWikiCode.text = langCode
                    }
                }
                binding.notificationSource.isVisible = true
                binding.notificationWikiCodeContainer.isVisible = true
            } ?: run {
                binding.notificationSource.isVisible = false
                binding.notificationSourceExternalIcon.isVisible = false
                binding.notificationWikiCodeBackground.isVisible = false
                binding.notificationWikiCodeContainer.isVisible = false
            }

            if (container.selected) {
                binding.notificationItemSelectedImage.visibility = View.VISIBLE
                binding.notificationItemImage.visibility = View.INVISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.multi_select_background_color))
            } else {
                binding.notificationItemSelectedImage.visibility = View.INVISIBLE
                binding.notificationItemImage.visibility = View.VISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.paper_color))
            }

            binding.notificationOverflowMenu.setOnClickListener {
                // TODO: implement this
            }
        }

        override fun onClick(v: View) {
            if (MultiSelectActionModeCallback.isTagType(actionMode)) {
                toggleSelectItem(container)
            } else {
                val n = container.notification!!
                n.contents?.links?.getPrimary()?.let { link ->
                    val url = link.url
                    if (url.isNotEmpty()) {
                        NotificationInteractionFunnel(WikipediaApp.getInstance(), n).logAction(NotificationInteractionEvent.ACTION_PRIMARY, link)
                        NotificationInteractionEvent.logAction(n, NotificationInteractionEvent.ACTION_PRIMARY, link)
                        linkHandler.wikiSite = WikiSite(url)
                        linkHandler.onUrlClick(url, null, "")
                    }
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            toggleSelectItem(container)
            beginMultiSelect()
            return true
        }

        override fun onSwipe() {
            markReadItems(listOf(container), false)
        }
    }

    private inner class NotificationSearchBarHolder constructor(view: View) :
        RecyclerView.ViewHolder(view) {
        val notificationFilterButton: AppCompatImageView = itemView.findViewById(R.id.notification_filter_button)
        val notificationFilterCountView: TextView = itemView.findViewById(R.id.notification_filter_count)

        init {
            (itemView as WikiCardView).setCardBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.color_group_22))

            updateFilterIconAndCount()

            itemView.setOnClickListener {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback)
                    postprocessAndDisplay()
                }
            }

            notificationFilterButton.setOnClickListener {
                startActivity(NotificationsFilterActivity.newIntent(it.context))
            }

            FeedbackUtil.setButtonLongPressToast(notificationFilterButton)
        }

        private fun updateFilterIconAndCount() {
            val fullWikiAndTypeListSize = NotificationsFilterActivity.allWikisList().size + NotificationsFilterActivity.allTypesIdList().size
            val delimitedFiltersSizeString = Prefs.notificationsFilterLanguageCodes.orEmpty().split(",").filter { it.isNotEmpty() }.size
            val enabledFilters = fullWikiAndTypeListSize - delimitedFiltersSizeString
            if (enabledFilters == 0) {
                notificationFilterCountView.visibility = View.GONE
                notificationFilterButton.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.chip_text_color))
            } else {
                notificationFilterCountView.visibility = View.VISIBLE
                notificationFilterCountView.text = enabledFilters.toString()
                notificationFilterButton.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.colorAccent))
            }
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
            if (type == NotificationListItemContainer.ITEM_SEARCH_BAR) {
                return NotificationSearchBarHolder(layoutInflater.inflate(R.layout.view_notification_search_bar, parent, false))
            }
            return NotificationItemHolder(ItemNotificationBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            when (holder) {
                is NotificationItemHolder -> holder.bindItem(notificationContainerList[pos])
            }

            // if we're at the bottom of the list, and we have a continuation string, then execute it.
            if (pos == notificationContainerList.size - 1 && !currentContinueStr.isNullOrEmpty()) {
                orContinueNotifications
            }
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchAndFilterActionProvider: SearchAndFilterActionProvider? = null
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchAndFilterActionProvider = SearchAndFilterActionProvider(parentContext, searchHintString,
                object : SearchAndFilterActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchAndFilterActionProvider)

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

        fun refreshProvider() {
            searchAndFilterActionProvider?.updateFilterIconAndText()
        }
    }

    private inner class MultiSelectCallback : MultiSelectActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onCreateActionMode(mode, menu)
            mode.title = notificationContainerList.count { it.selected }.toString()
            mode.menuInflater.inflate(R.menu.menu_action_mode_notifications, menu)
            val isFirstItemUnread = notificationContainerList
                .filterNot { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }
                .first { it.selected }.notification?.isUnread
            menu.findItem(R.id.menu_mark_as_read).isVisible = isFirstItemUnread == true
            menu.findItem(R.id.menu_mark_as_unread).isVisible = isFirstItemUnread == false
            menu.findItem(R.id.menu_check_all).isVisible = true
            menu.findItem(R.id.menu_uncheck_all).isVisible = false
            actionMode = mode
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_mark_as_read -> {
                    markReadItems(selectedItems, false)
                    finishActionMode()
                    return true
                }
                R.id.menu_mark_as_unread -> {
                    markReadItems(selectedItems, true)
                    finishActionMode()
                    return true
                }
                R.id.menu_check_all -> {
                    checkAllItems(mode, true)
                    return true
                }
                R.id.menu_uncheck_all -> {
                    checkAllItems(mode, false)
                    return true
                }
            }
            return false
        }

        override fun onDeleteSelected() {
            // ignore
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            checkAllItems(mode, false)
            actionMode = null
            super.onDestroyActionMode(mode)
        }

        private fun checkAllItems(mode: ActionMode, check: Boolean) {
            notificationContainerList.map { it.selected = check }
            mode.title = selectedItemCount.toString()
            mode.menu.findItem(R.id.menu_check_all).isVisible = !check
            mode.menu.findItem(R.id.menu_uncheck_all).isVisible = check
            binding.notificationsRecyclerView.adapter!!.notifyDataSetChanged()
        }
    }

    private inner class NotificationLinkHandler constructor(context: Context) : LinkHandler(context) {

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // ignore
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            // ignore
        }

        override lateinit var wikiSite: WikiSite

        override fun onInternalLinkClicked(title: PageTitle) {
            startActivity(PageActivity.newIntentForCurrentTab(this@NotificationActivity,
                HistoryEntry(title, HistoryEntry.SOURCE_NOTIFICATION), title))
        }

        override fun onExternalLinkClicked(uri: Uri) {
            try {
                // TODO: handle "change password" since it will open a blank page in PageActivity
                startActivity(Intent(Intent.ACTION_VIEW).setData(uri))
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    private class NotificationListItemContainer {
        val type: Int
        var notification: Notification? = null
        var selected = false

        constructor() {
            type = ITEM_SEARCH_BAR
        }

        constructor(notification: Notification) {
            this.notification = notification
            type = ITEM_NOTIFICATION
        }

        companion object {
            const val ITEM_SEARCH_BAR = 0
            const val ITEM_NOTIFICATION = 1
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationActivity::class.java)
        }
    }
}
