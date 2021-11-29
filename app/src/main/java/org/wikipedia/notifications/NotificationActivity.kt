package org.wikipedia.notifications

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.view.*
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.PluralsRes
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collect
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.NotificationInteractionFunnel
import org.wikipedia.analytics.NotificationPreferencesFunnel
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.databinding.ActivityNotificationsBinding
import org.wikipedia.databinding.ItemNotificationBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.NotificationSettingsActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.DeviceUtil.setContextClickAsLongClick
import org.wikipedia.util.log.L
import org.wikipedia.views.*
import java.util.*

class NotificationActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationViewModel by viewModels()

    private lateinit var externalLinkIcon: Drawable
    private val notificationContainerList = mutableListOf<NotificationListItemContainer>()
    private var fromContinuation: Boolean = false
    private var actionMode: ActionMode? = null
    private val multiSelectActionModeCallback = MultiSelectCallback()
    private val searchActionModeCallback = SearchCallback()
    private var linkHandler = NotificationLinkHandler(this)
    private var notificationActionOverflowView: NotificationActionsOverflowView? = null
    private val typefaceSansSerifBold = Typeface.create("sans-serif", Typeface.BOLD)
    // TODO: maybe making the result observable and put into ViewModel class?
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == NotificationFilterActivity.ACTIVITY_RESULT_LANGUAGES_CHANGED) {
            beginUpdateList()
        } else {
            viewModel.updateTabSelection(binding.notificationTabLayout.selectedTabPosition)
        }
    }
    var funnel = NotificationPreferencesFunnel(WikipediaApp.getInstance())

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.notificationsToolbar)
        supportActionBar?.title = getString(R.string.notifications_activity_title)

        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground))
        binding.notificationsErrorView.retryClickListener = View.OnClickListener { beginUpdateList() }
        binding.notificationsErrorView.backClickListener = View.OnClickListener { onBackPressed() }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecyclerView.adapter = NotificationItemAdapter()
        binding.notificationsRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, skipSearchBar = true))

        externalLinkIcon = ContextCompat.getDrawable(this, R.drawable.ic_open_in_new_black_24px)?.apply {
            setBounds(0, 0, DimenUtil.roundedDpToPx(16f), DimenUtil.roundedDpToPx(16f))
        }!!

        val touchCallback = SwipeableItemTouchHelperCallback(this,
                ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent),
                R.drawable.ic_outline_drafts_24, android.R.color.white, true, binding.notificationsRefreshView)

        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.notificationsRecyclerView)

        binding.notificationsRefreshView.setOnRefreshListener {
            binding.notificationsRefreshView.isRefreshing = false
            finishActionMode()
            beginUpdateList()
        }

        binding.notificationTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.updateTabSelection(tab.position)
                finishActionMode()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        binding.notificationsSearchEmptyContainer.setOnClickListener {
            resultLauncher.launch(NotificationFilterActivity.newIntent(it.context))
        }

        Prefs.notificationUnreadCount = 0

        beginUpdateList()

        // TODO: use repeatOnLifecycle if the it is stable
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect {
                when (it) {
                    is NotificationViewModel.UiState.Success -> onNotificationsComplete(it.notifications, it.fromContinuation)
                    is NotificationViewModel.UiState.Error -> setErrorState(it.throwable)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionMode?.let {
            postprocessAndDisplay()
            if (SearchActionModeCallback.`is`(it)) {
                searchActionModeCallback.refreshProvider()
            }
        }
    }

    override fun onStop() {
        binding.root.requestFocus()
        notificationActionOverflowView?.dismiss()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_notifications_mark_all_as_read).isVisible = viewModel.allUnreadCount > 0
        return super.onPrepareOptionsMenu(menu)
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
                resultLauncher.launch(NotificationSettingsActivity.newIntent(this))
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
        viewModel.fetchAndSave()
    }

    private fun setSuccessState() {
        binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsErrorView.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.VISIBLE
        binding.notificationTabLayout.visibility = View.VISIBLE
    }

    private fun setErrorState(throwable: Throwable) {
        L.e(throwable)
        binding.notificationsProgressBar.visibility = View.GONE
        binding.notificationsRecyclerView.visibility = View.GONE
        binding.notificationsEmptyContainer.visibility = View.GONE
        binding.notificationsSearchEmptyContainer.visibility = View.GONE
        binding.notificationsErrorView.setError(throwable)
        binding.notificationsErrorView.visibility = View.VISIBLE
    }

    private fun onNotificationsComplete(notifications: List<NotificationListItemContainer>,
                                        fromContinuation: Boolean) {
        setSuccessState()
        this.fromContinuation = fromContinuation

        notificationContainerList.clear()
        notificationContainerList.addAll(notifications)
        postprocessAndDisplay()
    }

    private fun postprocessAndDisplay(position: Int? = null) {
        val allTab = binding.notificationTabLayout.getTabAt(0)!!
        val allUnreadCount = viewModel.allUnreadCount
        if (allUnreadCount > 0) {
            allTab.text = getString(R.string.notifications_tab_filter_all) + " " +
                    getString(R.string.notifications_tab_filter_unread, allUnreadCount.toString())
        } else {
            allTab.text = getString(R.string.notifications_tab_filter_all)
        }

        val mentionsTab = binding.notificationTabLayout.getTabAt(1)!!
        val mentionsUnreadCount = viewModel.mentionsUnreadCount
        if (mentionsUnreadCount > 0) {
            mentionsTab.text = getString(R.string.notifications_tab_filter_mentions) + " " +
                    getString(R.string.notifications_tab_filter_unread, mentionsUnreadCount.toString())
        } else {
            mentionsTab.text = getString(R.string.notifications_tab_filter_mentions)
        }

        // Handle search bar and TabLayout visibility
        binding.notificationTabLayout.visibility = if (actionMode != null) View.GONE else View.VISIBLE
        if (actionMode != null) {
            if (notificationContainerList.any { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }) {
                notificationContainerList.removeAt(0)
            }
        } else {
            if (notificationContainerList.none { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }) {
                notificationContainerList.add(0, NotificationListItemContainer())
            }
        }

        if (notificationContainerList.filterNot { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }.isEmpty()) {
            binding.notificationsEmptyContainer.visibility = if (actionMode == null && viewModel.excludedFiltersCount() == 0) View.VISIBLE else View.GONE
            binding.notificationsSearchEmptyContainer.visibility = if (viewModel.excludedFiltersCount() != 0) View.VISIBLE else View.GONE
            binding.notificationsSearchEmptyText.visibility = if (actionMode != null) View.VISIBLE else View.GONE
            binding.notificationsEmptySearchMessage.setText(getSpannedEmptySearchMessage(), TextView.BufferType.SPANNABLE)
        } else {
            binding.notificationsEmptyContainer.visibility = View.GONE
            binding.notificationsSearchEmptyContainer.visibility = View.GONE
            binding.notificationsSearchEmptyText.visibility = View.GONE
        }

        invalidateOptionsMenu()
        if (position != null) {
            binding.notificationsRecyclerView.adapter?.notifyItemChanged(position)
        } else {
            binding.notificationsRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun getSpannedEmptySearchMessage(): Spannable {
        val filtersStr = resources.getQuantityString(R.plurals.notifications_number_of_filters, viewModel.excludedFiltersCount(), viewModel.excludedFiltersCount())
        val emptySearchMessage = getString(R.string.notifications_empty_search_message, filtersStr)
        val spannable = SpannableString(emptySearchMessage)
        val prefixStringLength = 13
        spannable.setSpan(ForegroundColorSpan(ResourceUtil.getThemedColor(this, R.attr.colorAccent)), prefixStringLength, prefixStringLength + filtersStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private fun markReadItems(items: List<NotificationListItemContainer>, markUnread: Boolean, fromUndoOrClick: Boolean = false, position: Int? = null) {
        if (!WikipediaApp.getInstance().isOnline) {
            if (fromUndoOrClick && position != null) {
                // Skip if the action is from onClick.
                return
            }
            Toast.makeText(this, R.string.notifications_offline_disable_message, Toast.LENGTH_SHORT).show()
        } else {
            viewModel.markItemsAsRead(items, markUnread)
            if (!fromUndoOrClick) {
                showMarkReadItemsUndoSnackbar(items, markUnread)
            }
        }

        finishActionMode()
        postprocessAndDisplay(position)
    }

    private fun showMarkReadItemsUndoSnackbar(items: List<NotificationListItemContainer>, markUnread: Boolean) {
        @PluralsRes val snackbarStringRes = if (markUnread) R.plurals.notifications_mark_as_unread_plural else R.plurals.notifications_mark_as_read_plural
        FeedbackUtil.makeSnackbar(this, resources.getQuantityString(snackbarStringRes, items.size, items.size), FeedbackUtil.LENGTH_DEFAULT)
                .setAction(R.string.notification_archive_undo) { markReadItems(items, !markUnread, true) }
                .show()
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

    private fun toggleSelectItem(container: NotificationListItemContainer, position: Int) {
        container.selected = !container.selected
        if (selectedItemCount == 0) {
            finishActionMode()
        }
        actionMode?.invalidate()
        binding.notificationsRecyclerView.adapter?.notifyItemChanged(position)
    }

    private val selectedItemCount get() = notificationContainerList.count { it.selected }

    private val selectedItems get() = notificationContainerList.filterNot { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }.filter { it.selected }

    private inner class NotificationItemHolder constructor(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener, SwipeableItemTouchHelperCallback.Callback {

        lateinit var container: NotificationListItemContainer
        var itemPosition = -1

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            setContextClickAsLongClick(itemView)
        }

        fun bindItem(container: NotificationListItemContainer, pos: Int) {
            this.container = container
            this.itemPosition = pos
            val n = container.notification!!
            val notificationCategory = NotificationCategory.find(n.category)
            val notificationColor = ContextCompat.getColor(this@NotificationActivity,
                ResourceUtil.getThemedAttributeId(this@NotificationActivity, notificationCategory.iconColor))
            val primaryColor = ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.primary_text_color)

            binding.notificationItemImage.setImageResource(notificationCategory.iconResId)
            binding.notificationItemImage.setColorFilter(if (n.isUnread) notificationColor else
                ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.toolbar_icon_color), PorterDuff.Mode.SRC_IN)
            n.contents?.let {
                binding.notificationSubtitle.text = RichTextUtil.stripHtml(it.header)
                StringUtil.highlightAndBoldenText(binding.notificationSubtitle, viewModel.currentSearchQuery, true, Color.YELLOW)
                if (it.body.trim().isNotEmpty() && it.body.trim().isNotBlank()) {
                    binding.notificationDescription.text = RichTextUtil.stripHtml(it.body)
                    StringUtil.highlightAndBoldenText(binding.notificationDescription, viewModel.currentSearchQuery, true, Color.YELLOW)
                    binding.notificationDescription.visibility = View.VISIBLE
                } else {
                    binding.notificationDescription.visibility = View.GONE
                }
                it.links?.secondary?.firstOrNull()?.let { link ->
                    binding.notificationTitle.text = link.label
                    StringUtil.highlightAndBoldenText(binding.notificationTitle, viewModel.currentSearchQuery, true, Color.YELLOW)
                } ?: run {
                    binding.notificationTitle.text = getString(notificationCategory.title)
                }
            }

            // TODO: use better diff date method
            binding.notificationTime.text = DateUtils.getRelativeTimeSpanString(n.date().time, System.currentTimeMillis(), 0L)

            binding.notificationTitle.typeface = if (n.isUnread) typefaceSansSerifBold else Typeface.DEFAULT
            binding.notificationTitle.setTextColor(if (n.isUnread) notificationColor else primaryColor)
            binding.notificationSubtitle.typeface = if (n.isUnread) typefaceSansSerifBold else Typeface.DEFAULT

            val langCode = StringUtil.dbNameToLangCode(n.wiki)
            L10nUtil.setConditionalLayoutDirection(itemView, langCode)

            n.title?.let { title ->
                binding.notificationSource.text = title.full
                StringUtil.highlightAndBoldenText(binding.notificationSource, viewModel.currentSearchQuery, true, Color.YELLOW)
                n.contents?.links?.getPrimary()?.url?.let {
                    binding.notificationSource.setCompoundDrawables(null, null,
                            if (UriUtil.isAppSupportedLink(Uri.parse(it))) null else externalLinkIcon, null)
                }
                val params = binding.notificationSource.layoutParams as ConstraintLayout.LayoutParams
                params.setMargins(DimenUtil.roundedDpToPx(8f), 0, 0, 0)
                binding.notificationSource.layoutParams = params

                when {
                    langCode == Constants.WIKI_CODE_WIKIDATA -> {
                        binding.notificationWikiCode.visibility = View.GONE
                        binding.notificationWikiCodeImage.visibility = View.VISIBLE
                        binding.notificationWikiCodeImage.setImageResource(R.drawable.ic_wikidata_logo)
                        binding.notificationWikiCodeContainer.isVisible = true
                    }
                    langCode == Constants.WIKI_CODE_COMMONS -> {
                        binding.notificationWikiCode.visibility = View.GONE
                        binding.notificationWikiCodeImage.visibility = View.VISIBLE
                        binding.notificationWikiCodeImage.setImageResource(R.drawable.ic_commons_logo)
                        binding.notificationWikiCodeContainer.isVisible = true
                    }
                    isValidAppLanguageCode(langCode) -> {
                        binding.notificationWikiCode.visibility = View.VISIBLE
                        binding.notificationWikiCodeImage.visibility = View.GONE
                        binding.notificationWikiCodeContainer.isVisible = true
                        binding.notificationWikiCode.text = langCode
                        ViewUtil.formatLangButton(binding.notificationWikiCode, langCode,
                            SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_MEDIUM)
                    }
                    else -> {
                        params.setMargins(0, DimenUtil.roundedDpToPx(12f), 0, 0)
                        binding.notificationSource.layoutParams = params
                        binding.notificationWikiCodeContainer.isVisible = false
                    }
                }
                binding.notificationSource.isVisible = true
            } ?: run {
                binding.notificationSource.isVisible = false
                binding.notificationSource.setCompoundDrawables(null, null, null, null)
                binding.notificationWikiCodeContainer.isVisible = false
            }

            if (container.selected) {
                binding.notificationItemSelectedImage.visibility = View.VISIBLE
                binding.notificationItemImage.visibility = View.INVISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.multi_select_background_color))
                if (WikipediaApp.getInstance().currentTheme.isDark) {
                    binding.notificationTitle.setTextColor(Color.WHITE)
                }
            } else {
                binding.notificationItemSelectedImage.visibility = View.INVISIBLE
                binding.notificationItemImage.visibility = View.VISIBLE
                itemView.setBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.paper_color))
            }

            // setting tag for swipe action text
            if (n.isUnread) {
                itemView.setTag(R.string.tag_text_key, getString(R.string.notifications_swipe_action_read))
                itemView.setTag(R.string.tag_icon_key, R.drawable.ic_outline_drafts_24)
            } else {
                itemView.setTag(R.string.tag_text_key, getString(R.string.notifications_swipe_action_unread))
                itemView.setTag(R.string.tag_icon_key, R.drawable.ic_outline_email_24)
            }

            binding.notificationOverflowMenu.setOnClickListener {
                showOverflowMenu(it)
            }
        }

        private fun isValidAppLanguageCode(langCode: String): Boolean {
            return WikipediaApp.getInstance().language().getLanguageCodeIndex(langCode) >= 0 ||
                    WikipediaApp.getInstance().language().getLanguageVariants(langCode) != null
        }

        override fun onClick(v: View) {
            if (MultiSelectActionModeCallback.isTagType(actionMode)) {
                toggleSelectItem(container, itemPosition)
            } else {
                val n = container.notification!!
                markReadItems(listOf(container), markUnread = false, fromUndoOrClick = true, position = itemPosition)
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
            beginMultiSelect()
            toggleSelectItem(container, itemPosition)
            return true
        }

        override fun onSwipe() {
            container.notification?.let {
                markReadItems(listOf(container), !it.isUnread, position = itemPosition)
            }
        }

        private fun showOverflowMenu(anchorView: View) {
            notificationActionOverflowView = NotificationActionsOverflowView(this@NotificationActivity)
            notificationActionOverflowView?.show(anchorView, container) {
                    container, markRead -> markReadItems(listOf(container), !markRead, position = itemPosition)
            }
        }
    }

    private inner class NotificationSearchBarHolder constructor(view: View) :
        RecyclerView.ViewHolder(view) {
        val notificationFilterButton: AppCompatImageView = itemView.findViewById(R.id.notification_filter_button)
        val notificationFilterCountView: TextView = itemView.findViewById(R.id.notification_filter_count)

        init {
            (itemView as WikiCardView).setCardBackgroundColor(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.color_group_22))

            itemView.setOnClickListener {
                if (actionMode == null) {
                    funnel.logSearchClick()
                    actionMode = startSupportActionMode(searchActionModeCallback)
                    postprocessAndDisplay()
                }
            }

            notificationFilterButton.setOnClickListener {
                funnel.logFilterClick()
                resultLauncher.launch(NotificationFilterActivity.newIntent(it.context))
            }

            FeedbackUtil.setButtonLongPressToast(notificationFilterButton)
        }

        fun updateFilterIconAndCount() {
            val excludedFilters = viewModel.excludedFiltersCount()
            if (excludedFilters == 0) {
                notificationFilterCountView.visibility = View.GONE
                ImageViewCompat.setImageTintList(notificationFilterButton,
                    ColorStateList.valueOf(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.chip_text_color)))
            } else {
                notificationFilterCountView.visibility = View.VISIBLE
                notificationFilterCountView.text = excludedFilters.toString()
                ImageViewCompat.setImageTintList(notificationFilterButton,
                    ColorStateList.valueOf(ResourceUtil.getThemedColor(this@NotificationActivity, R.attr.colorAccent)))
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
                is NotificationItemHolder -> holder.bindItem(notificationContainerList[pos], pos)
                is NotificationSearchBarHolder -> holder.updateFilterIconAndCount()
            }

            // if we're at the bottom of the list, and we have a continuation string, then execute it.
            if (pos == notificationContainerList.size - 1 && fromContinuation) {
                viewModel.fetchAndSave()
            }
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {

        var searchAndFilterActionProvider: SearchAndFilterActionProvider? = null
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchAndFilterActionProvider = SearchAndFilterActionProvider(this@NotificationActivity, searchHintString,
                object : SearchAndFilterActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }

                    override fun getExcludedFilterCount(): Int {
                        return viewModel.excludedFiltersCount()
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchAndFilterActionProvider)

            actionMode = mode
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.updateSearchQuery(s.trim())
            postprocessAndDisplay()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.updateSearchQuery(null)
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
            mode.menuInflater.inflate(R.menu.menu_action_mode_notifications, menu)
            actionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = selectedItemCount.toString()
            val isFirstItemUnread = selectedItems.firstOrNull()?.notification?.isUnread
            menu.findItem(R.id.menu_mark_as_read).isVisible = isFirstItemUnread == true
            menu.findItem(R.id.menu_mark_as_unread).isVisible = isFirstItemUnread == false
            menu.findItem(R.id.menu_check_all).isVisible = true
            menu.findItem(R.id.menu_uncheck_all).isVisible = false
            return super.onPrepareActionMode(mode, menu)
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
                    finishActionMode()
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
            notificationContainerList
                .filterNot { it.type == NotificationListItemContainer.ITEM_SEARCH_BAR }
                .map { it.selected = check }
            mode.title = selectedItemCount.toString()
            mode.menu.findItem(R.id.menu_check_all).isVisible = !check
            mode.menu.findItem(R.id.menu_uncheck_all).isVisible = check
            binding.notificationsRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationActivity::class.java)
        }
    }
}
