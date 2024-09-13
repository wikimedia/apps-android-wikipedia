package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource.TALK_TOPICS_ACTIVITY
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTalkTopicsBinding
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.databinding.ViewTalkTopicsHeaderBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.edit.EditHandler
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.NotificationButtonView
import org.wikipedia.views.SearchActionProvider
import org.wikipedia.views.SwipeableItemTouchHelperCallback
import org.wikipedia.views.TalkTopicsSortOverflowView
import org.wikipedia.views.ViewUtil
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog

class TalkTopicsActivity : BaseActivity(), WatchlistExpiryDialog.Callback {
    private lateinit var binding: ActivityTalkTopicsBinding
    private lateinit var invokeSource: Constants.InvokeSource
    private lateinit var notificationButtonView: NotificationButtonView
    private val viewModel: TalkTopicsViewModel by viewModels { TalkTopicsViewModel.Factory(intent.parcelableExtra(Constants.ARG_TITLE)!!) }
    private val concatAdapter = ConcatAdapter()
    private val headerAdapter = HeaderItemAdapter()
    private val talkTopicItemAdapter = TalkTopicItemAdapter()
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()
    private var goToTopic = false

    private val requestLanguageChange = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.let { intent ->
                if (intent.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                    val pos = intent.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
                    if (pos < WikipediaApp.instance.languageState.appLanguageCodes.size) {

                        val newNamespace = when {
                            viewModel.pageTitle.namespace() == Namespace.USER -> {
                                UserAliasData.valueFor(WikipediaApp.instance.languageState.appLanguageCodes[pos])
                            }
                            viewModel.pageTitle.namespace() == Namespace.USER_TALK -> {
                                UserTalkAliasData.valueFor(WikipediaApp.instance.languageState.appLanguageCodes[pos])
                            }
                            else -> viewModel.pageTitle.namespace
                        }

                        val newPageTitle = PageTitle(newNamespace, StringUtil.removeNamespace(viewModel.pageTitle.prefixedText),
                            WikiSite.forLanguageCode(WikipediaApp.instance.languageState.appLanguageCodes[pos]))

                        resetViews()
                        viewModel.updatePageTitle(newPageTitle)
                    }
                }
            }
        }
    }

    private val requestNewTopic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS) {
            val newRevisionId = it.data?.getLongExtra(TalkReplyActivity.RESULT_NEW_REVISION_ID, 0) ?: 0
            val undoneSubject = it.data?.getCharSequenceExtra(TalkReplyActivity.EXTRA_SUBJECT) ?: ""
            val undoneText = it.data?.getCharSequenceExtra(TalkReplyActivity.EXTRA_BODY) ?: ""
            if (newRevisionId > 0) {
                FeedbackUtil.makeSnackbar(this, getString(R.string.talk_new_topic_submitted))
                    .setAnchorView(binding.talkNewTopicButton)
                    .setAction(R.string.talk_snackbar_undo) {
                        binding.talkNewTopicButton.isEnabled = false
                        binding.talkNewTopicButton.alpha = 0.5f
                        binding.talkProgressBar.isVisible = true
                        binding.talkConditionContainer.isVisible = true
                        viewModel.undoSave(newRevisionId, undoneSubject, undoneText)
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar, @DismissEvent event: Int) {
                            if (!isDestroyed) {
                                AccountUtil.maybeShowTempAccountWelcome(this@TalkTopicsActivity)
                            }
                        }
                    })
                    .show()
                viewModel.loadTopics()
            }
        } else {
            // Make sure to reload the list in the case of exiting from the undo process.
            viewModel.loadTopics()
        }
    }

    private val requestEditSource = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            viewModel.loadTopics()
        }
    }

    private val requestGoToTopic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        finish()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        goToTopic = intent.getBooleanExtra(EXTRA_GO_TO_TOPIC, false)
        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_divider, drawStart = false, drawEnd = false, skipSearchBar = true))
        binding.talkRecyclerView.itemAnimator = null

        val touchCallback = SwipeableItemTouchHelperCallback(this,
            ResourceUtil.getThemedAttributeId(this, R.attr.progressive_color),
            R.drawable.ic_outline_drafts_24, android.R.color.white, true, binding.talkRefreshView)

        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.talkRecyclerView)

        binding.talkErrorView.backClickListener = View.OnClickListener {
            finish()
        }
        binding.talkErrorView.retryClickListener = View.OnClickListener {
            resetViews()
            viewModel.loadTopics()
        }

        binding.talkNewTopicButton.setOnClickListener {
            requestNewTopic.launch(TalkReplyActivity.newIntent(this@TalkTopicsActivity, viewModel.pageTitle, null, null, TALK_TOPICS_ACTIVITY))
        }

        binding.talkRefreshView.setOnRefreshListener {
            binding.talkRefreshView.isRefreshing = false
            resetViews()
            viewModel.loadTopics()
        }
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.progressive_color))

        invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource

        binding.talkNewTopicButton.isVisible = false

        notificationButtonView = NotificationButtonView(this)
        Prefs.hasAnonymousNotification = false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is TalkTopicsViewModel.UiState.UpdateNamespace -> setToolbarTitle(it.pageTitle)
                            is TalkTopicsViewModel.UiState.LoadTopic -> updateOnSuccess(it.pageTitle, it.threadItems)
                            is TalkTopicsViewModel.UiState.LoadError -> updateOnError(it.throwable)
                        }
                    }
                }

                launch {
                    viewModel.actionState.collect {
                        when (it) {
                            is TalkTopicsViewModel.ActionState.UndoEdit -> updateOnUndoSave(it.undoneSubject, it.undoneBody)
                            is TalkTopicsViewModel.ActionState.DoWatch -> updateOnWatch()
                            is TalkTopicsViewModel.ActionState.OnError -> FeedbackUtil.showError(this@TalkTopicsActivity, it.throwable)
                        }
                    }
                }
            }
        }
        resetViews()
    }

    public override fun onResume() {
        super.onResume()
        searchActionModeCallback.searchActionProvider?.selectAllQueryTexts()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!goToTopic) {
            menuInflater.inflate(R.menu.menu_talk, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (!goToTopic) {
            menu.findItem(R.id.menu_change_language).isVisible = viewModel.pageTitle.namespace() == Namespace.USER_TALK
            menu.findItem(R.id.menu_read_article).isVisible = viewModel.pageTitle.namespace() != Namespace.USER_TALK && invokeSource != Constants.InvokeSource.ARCHIVED_TALK_ACTIVITY
            menu.findItem(R.id.menu_view_user_page).isVisible = viewModel.pageTitle.namespace() == Namespace.USER_TALK
            menu.findItem(R.id.menu_view_user_page).title = getString(R.string.menu_option_user_page, StringUtil.removeHTMLTags(StringUtil.removeNamespace(viewModel.pageTitle.displayText)))
            menu.findItem(R.id.menu_edit_source)?.isVisible = AccountUtil.isLoggedIn

            val notificationMenuItem = menu.findItem(R.id.menu_notifications)
            val watchMenuItem = menu.findItem(R.id.menu_watch)
            if (AccountUtil.isLoggedIn) {
                // Notification
                notificationMenuItem.isVisible = true
                notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
                notificationButtonView.setOnClickListener {
                    if (AccountUtil.isLoggedIn) {
                        startActivity(NotificationActivity.newIntent(this))
                    }
                }
                notificationButtonView.contentDescription =
                    getString(R.string.notifications_activity_title)
                notificationMenuItem.actionView = notificationButtonView
                notificationMenuItem.expandActionView()
                FeedbackUtil.setButtonTooltip(notificationButtonView)

                // Watchlist
                watchMenuItem.isVisible = true
                watchMenuItem.title = getString(if (viewModel.isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
                watchMenuItem.setIcon(PageActionItem.watchlistIcon(viewModel.isWatched, viewModel.hasWatchlistExpiry))
            } else {
                notificationMenuItem.isVisible = false
                watchMenuItem.isVisible = false
            }
            updateNotificationDot(false)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_change_language -> {
                requestLanguageChange.launch(WikipediaLanguagesActivity.newIntent(this, Constants.InvokeSource.TALK_TOPICS_ACTIVITY))
                true
            }
            R.id.menu_read_article, R.id.menu_view_user_page -> {
                goToPage()
                true
            }
            R.id.menu_view_edit_history -> {
                startActivity(EditHistoryListActivity.newIntent(this, viewModel.pageTitle))
                true
            }
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_talk_page), viewModel.pageTitle.uri)
                true
            }
            R.id.menu_watch -> {
                if (AccountUtil.isLoggedIn) {
                    viewModel.watchOrUnwatch(WatchlistExpiry.NEVER, viewModel.isWatched)
                }
                true
            }
            R.id.menu_edit_source -> {
                requestEditSource.launch(
                    EditSectionActivity.newIntent(this, -1, null,
                        viewModel.pageTitle, Constants.InvokeSource.TALK_TOPICS_ACTIVITY))
                true
            }
            R.id.menu_archive -> {
                startActivity(ArchivedTalkPagesActivity.newIntent(this, viewModel.pageTitle))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onUnreadNotification() {
        updateNotificationDot(true)
    }

    override fun onExpiryChanged(expiry: WatchlistExpiry) {
        viewModel.hasWatchlistExpiry = expiry !== WatchlistExpiry.NEVER
        invalidateOptionsMenu()
    }

    private fun resetViews() {
        invalidateOptionsMenu()
        L10nUtil.setConditionalLayoutDirection(binding.talkContentsView, viewModel.pageTitle.wikiSite.languageCode)
        binding.talkProgressBar.isVisible = true
        binding.talkErrorView.isVisible = false
        binding.talkEmptyContainer.isVisible = false
        binding.talkConditionContainer.isVisible = true
    }

    private fun updateOnSuccess(pageTitle: PageTitle, threadItems: List<ThreadItem>) {
        setToolbarTitle(pageTitle)

        if (binding.talkRecyclerView.adapter == null) {
            binding.talkRecyclerView.adapter = concatAdapter.apply {
                addAdapter(0, headerAdapter)
                addAdapter(1, talkTopicItemAdapter)
            }
        }

        if (intent.getBooleanExtra(EXTRA_GO_TO_TOPIC, false)) {
            intent.putExtra(EXTRA_GO_TO_TOPIC, false)
            var threadTopic: ThreadItem? = null
            var threadItem: ThreadItem? = null
            if (!pageTitle.fragment.isNullOrEmpty()) {
                threadItems.forEach { topic ->
                    if (threadTopic == null) {
                        if (StringUtil.addUnderscores(StringUtil.fromHtml(topic.html).toString()) == pageTitle.fragment ||
                                topic.name == pageTitle.fragment) {
                            threadTopic = topic
                        } else {
                            threadItem = topic.allReplies.find { it.id == pageTitle.fragment }
                            if (threadItem != null) {
                                threadTopic = topic
                            }
                        }
                    }
                }
            }
            if (threadTopic != null) {
                requestGoToTopic.launch(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, threadTopic!!.name, threadTopic!!.id, threadItem?.id, viewModel.currentSearchQuery, invokeSource))
                overridePendingTransition(0, 0)
                return
            }
        }
        goToTopic = false
        if (threadItems.isEmpty()) {
            updateOnEmpty()
        } else {
            binding.talkErrorView.isVisible = false
            binding.talkConditionContainer.isVisible = false
            if (actionMode != null) {
                binding.talkNewTopicButton.hide()
            } else {
                binding.talkNewTopicButton.show()
                binding.talkNewTopicButton.isEnabled = true
                binding.talkNewTopicButton.alpha = 1.0f
            }
            talkTopicItemAdapter.notifyDataSetChanged()
        }
        binding.talkProgressBar.isVisible = false
        invalidateOptionsMenu()
    }

    private fun updateOnError(t: Throwable) {
        binding.talkProgressBar.isVisible = false
        binding.talkRecyclerView.adapter?.notifyDataSetChanged()

        // In the case of 404, it just means that the talk page hasn't been created yet.
        if (t is HttpStatusException && t.code == 404) {
            updateOnEmpty()
            invalidateOptionsMenu()
        } else {
            binding.talkNewTopicButton.hide()
            binding.talkConditionContainer.isVisible = true
            binding.talkErrorView.isVisible = true
            binding.talkErrorView.setError(t)
        }
    }

    private fun updateOnEmpty() {
        binding.talkProgressBar.isVisible = false
        binding.talkRefreshView.isRefreshing = false
        binding.talkEmptyContainer.isVisible = true
        binding.talkConditionContainer.isVisible = true
        // Allow them to create a new topic anyway
        binding.talkNewTopicButton.show()
    }

    private fun updateOnUndoSave(undoneSubject: CharSequence, undoneBody: CharSequence) {
        requestNewTopic.launch(TalkReplyActivity.newIntent(this@TalkTopicsActivity, viewModel.pageTitle, null, null, TALK_TOPICS_ACTIVITY, undoneSubject, undoneBody))
    }

    private fun updateOnWatch() {
        showWatchlistSnackbar()
        invalidateOptionsMenu()
    }

    private fun setToolbarTitle(pageTitle: PageTitle) {
        val title = StringUtil.fromHtml(pageTitle.namespace.ifEmpty { TalkAliasData.valueFor(pageTitle.wikiSite.languageCode) } + ": " + "<a href='#'>${StringUtil.removeNamespace(StringUtil.removeHTMLTags(pageTitle.displayText))}</a>")
        ViewUtil.getTitleViewFromToolbar(binding.toolbar)?.let {
            it.contentDescription = title
            it.isVisible = !goToTopic
            if (invokeSource != Constants.InvokeSource.ARCHIVED_TALK_ACTIVITY) {
                it.movementMethod = LinkMovementMethodExt { _ ->
                    goToPage()
                }
            }
            FeedbackUtil.setButtonTooltip(it)
        }
        supportActionBar?.title = title
    }

    private fun updateNotificationDot(animate: Boolean) {
        if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            if (animate) {
                notificationButtonView.runAnimation()
            }
        } else {
            notificationButtonView.setUnreadCount(0)
        }
    }

    private fun goToPage() {
        val entry = HistoryEntry(getNonTalkPageTitle(viewModel.pageTitle), HistoryEntry.SOURCE_TALK_TOPIC)
        startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
    }

    private fun showWatchlistSnackbar() {
        if (!viewModel.isWatched) {
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, viewModel.pageTitle.displayText))
        } else if (viewModel.isWatched) {
            val snackbar = FeedbackUtil.makeSnackbar(this,
                getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                    viewModel.pageTitle.displayText,
                    getString(WatchlistExpiry.NEVER.stringId)))
            snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                ExclusiveBottomSheetPresenter.show(supportFragmentManager, WatchlistExpiryDialog.newInstance(viewModel.pageTitle, WatchlistExpiry.NEVER))
            }
            snackbar.show()
        }
    }

    private fun updateConcatAdapter() {
        if (actionMode == null) {
            concatAdapter.apply {
                addAdapter(0, headerAdapter)
            }
        } else {
            concatAdapter.apply {
                removeAdapter(headerAdapter)
            }
        }
    }

    private inner class HeaderItemAdapter : RecyclerView.Adapter<HeaderViewHolder>() {
        override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
            holder.bindItem()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
            return HeaderViewHolder(ViewTalkTopicsHeaderBinding.inflate(layoutInflater, parent, false))
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class HeaderViewHolder constructor(private val binding: ViewTalkTopicsHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.searchContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(this@TalkTopicsActivity, R.attr.background_color))

            binding.searchContainer.setOnClickListener {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback)
                }
            }

            binding.talkSortButton.setOnClickListener {
                TalkTopicsSortOverflowView(this@TalkTopicsActivity).show(binding.talkSortButton, viewModel.currentSortMode) {
                    viewModel.currentSortMode = it
                    talkTopicItemAdapter.notifyDataSetChanged()
                }
            }

            FeedbackUtil.setButtonTooltip(binding.talkSortButton)
        }

        fun bindItem() {
            binding.talkLeadImageContainer.isVisible = viewModel.pageTitle.namespace() != Namespace.USER_TALK
            viewModel.pageTitle.thumbUrl?.let {
                binding.talkLeadImage.contentDescription = StringUtil.removeNamespace(viewModel.pageTitle.displayText)
                binding.talkLeadImage.loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)))
            } ?: run {
                binding.talkLeadImageContainer.isVisible = false
            }
        }
    }

    internal inner class TalkTopicItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int {
            return viewModel.sortedThreadItems.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            return TalkTopicHolder(ItemTalkTopicBinding.inflate(layoutInflater, parent, false), this@TalkTopicsActivity, viewModel, invokeSource)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is TalkTopicHolder) {
                holder.bindItem(viewModel.sortedThreadItems[pos])
            }
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {
        var searchActionProvider: SearchActionProvider? = null
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchActionProvider = SearchActionProvider(this@TalkTopicsActivity, getSearchHintString()) { onQueryChange(it) }

            val menuItem = menu.add(getSearchHintString())

            MenuItemCompat.setActionProvider(menuItem, searchActionProvider)

            actionMode = mode
            binding.talkNewTopicButton.hide()
            updateConcatAdapter()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.currentSearchQuery = s
            talkTopicItemAdapter.notifyDataSetChanged()
            binding.talkSearchNoResult.isVisible = binding.talkRecyclerView.adapter?.itemCount == 0
            binding.talkConditionContainer.isVisible = binding.talkSearchNoResult.isVisible
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.currentSearchQuery = null
            binding.talkNewTopicButton.show()
            binding.talkConditionContainer.isVisible = false
            binding.talkSearchNoResult.isVisible = false
            talkTopicItemAdapter.notifyDataSetChanged()
            updateConcatAdapter()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.talk_search_topics_hint)
        }

        override fun getParentContext(): Context {
            return this@TalkTopicsActivity
        }
    }

    companion object {
        private const val EXTRA_GO_TO_TOPIC = "goToTopic"

        fun newIntent(context: Context, pageTitle: PageTitle, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                .putExtra(Constants.ARG_TITLE, pageTitle)
                .putExtra(EXTRA_GO_TO_TOPIC, !pageTitle.fragment.isNullOrEmpty())
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }

        fun getNonTalkPageTitle(title: PageTitle): PageTitle {
            val newTitle = title.copy()
            if (title.namespace() == Namespace.USER_TALK) {
                newTitle.namespace = UserAliasData.valueFor(title.wikiSite.languageCode)
            } else if (title.namespace() == Namespace.TALK) {
                newTitle.namespace = ""
            }
            return newTitle
        }
    }
}
