package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.TalkFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTalkTopicsBinding
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.databinding.ViewTalkTopicsFooterBinding
import org.wikipedia.databinding.ViewTalkTopicsHeaderBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.*
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.*
import org.wikipedia.views.*
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog

class TalkTopicsActivity : BaseActivity(), WatchlistExpiryDialog.Callback {
    private lateinit var binding: ActivityTalkTopicsBinding
    private lateinit var invokeSource: Constants.InvokeSource
    private lateinit var notificationButtonView: NotificationButtonView
    private val viewModel: TalkTopicsViewModel by viewModels { TalkTopicsViewModel.Factory(intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!) }
    private val concatAdapter = ConcatAdapter()
    private val headerAdapter = HeaderItemAdapter()
    private val talkTopicItemAdapter = TalkTopicItemAdapter()
    private val footerAdapter = FooterItemAdapter()
    private var funnel: TalkFunnel? = null
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()
    private var goToTopic = false
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    private val requestLanguageChange = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.let { intent ->
                if (intent.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                    val pos = intent.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
                    if (pos < WikipediaApp.instance.languageState.appLanguageCodes.size) {
                        funnel?.logChangeLanguage()

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
                    .show()
                viewModel.loadTopics()
            }
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
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false, skipSearchBar = true))
        binding.talkRecyclerView.itemAnimator = null

        val touchCallback = SwipeableItemTouchHelperCallback(this,
            ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent),
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
            funnel?.logNewTopicClick()
            requestNewTopic.launch(TalkReplyActivity.newIntent(this@TalkTopicsActivity, viewModel.pageTitle, null, null, invokeSource))
        }

        binding.talkRefreshView.setOnRefreshListener {
            binding.talkRefreshView.isRefreshing = false
            funnel?.logRefresh()
            resetViews()
            viewModel.loadTopics()
        }
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent))

        invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource

        binding.talkNewTopicButton.isVisible = false

        notificationButtonView = NotificationButtonView(this)
        Prefs.hasAnonymousNotification = false

        lifecycleScope.launchWhenCreated {
            viewModel.uiState.collect {
                when (it) {
                    is TalkTopicsViewModel.UiState.UpdateNamespace -> setToolbarTitle(it.pageTitle)
                    is TalkTopicsViewModel.UiState.LoadTopic -> updateOnSuccess(it.pageTitle, it.threadItems)
                    is TalkTopicsViewModel.UiState.UndoEdit -> updateOnUndoSave(it.undoneSubject, it.undoneBody)
                    is TalkTopicsViewModel.UiState.DoWatch -> updateOnWatch()
                    is TalkTopicsViewModel.UiState.LoadError -> updateOnError(it.throwable)
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
            menu.findItem(R.id.menu_view_user_page).title = getString(R.string.menu_option_user_page, StringUtil.removeNamespace(viewModel.pageTitle.displayText))

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
                FeedbackUtil.setButtonLongPressToast(notificationButtonView)

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
        when (item.itemId) {
            R.id.menu_change_language -> {
                requestLanguageChange.launch(WikipediaLanguagesActivity.newIntent(this, Constants.InvokeSource.TALK_ACTIVITY))
                return true
            }
            R.id.menu_read_article, R.id.menu_view_user_page -> {
                goToPage()
                return true
            }
            R.id.menu_view_edit_history -> {
                startActivity(EditHistoryListActivity.newIntent(this, viewModel.pageTitle))
                return true
            }
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_talk_page), viewModel.pageTitle.uri)
                return true
            }
            R.id.menu_watch -> {
                if (AccountUtil.isLoggedIn) {
                    viewModel.watchOrUnwatch(WatchlistExpiry.NEVER, viewModel.isWatched)
                }
                return true
            }
            R.id.menu_archive -> {
                startActivity(ArchivedTalkPagesActivity.newIntent(this, viewModel.pageTitle))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onUnreadNotification() {
        updateNotificationDot(true)
    }

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        viewModel.watchOrUnwatch(expiry, false)
        bottomSheetPresenter.dismiss(supportFragmentManager)
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
        funnel = TalkFunnel(pageTitle, invokeSource)
        setToolbarTitle(pageTitle)

        if (binding.talkRecyclerView.adapter == null) {
            binding.talkRecyclerView.adapter = concatAdapter.apply {
                addAdapter(0, headerAdapter)
                addAdapter(1, talkTopicItemAdapter)
                addAdapter(2, footerAdapter)
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
            footerAdapter.notifyItemChanged(0)
            talkTopicItemAdapter.notifyDataSetChanged()
        }
        funnel?.logOpenTalk()

        binding.talkProgressBar.isVisible = false
        invalidateOptionsMenu()
    }

    private fun updateOnError(t: Throwable) {
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
        binding.talkEmptyContainer.isVisible = true
        binding.talkConditionContainer.isVisible = true
        // Allow them to create a new topic anyway
        binding.talkNewTopicButton.show()
    }

    private fun updateOnUndoSave(undoneSubject: CharSequence, undoneBody: CharSequence) {
        requestNewTopic.launch(TalkReplyActivity.newIntent(this@TalkTopicsActivity, viewModel.pageTitle, null, null, invokeSource, undoneSubject, undoneBody))
    }

    private fun updateOnWatch() {
        showWatchlistSnackbar()
        invalidateOptionsMenu()
    }

    private fun setToolbarTitle(pageTitle: PageTitle) {
        binding.toolbarTitle.text = StringUtil.fromHtml(pageTitle.namespace.ifEmpty { TalkAliasData.valueFor(pageTitle.wikiSite.languageCode) } + ": " + "<a href='#'>${StringUtil.removeNamespace(pageTitle.displayText)}</a>")
        binding.toolbarTitle.contentDescription = binding.toolbarTitle.text
        binding.toolbarTitle.isVisible = !goToTopic
        if (invokeSource != Constants.InvokeSource.ARCHIVED_TALK_ACTIVITY) {
            binding.toolbarTitle.movementMethod = LinkMovementMethodExt { _ ->
                goToPage()
            }
        }
        RichTextUtil.removeUnderlinesFromLinks(binding.toolbarTitle)
        FeedbackUtil.setButtonLongPressToast(binding.toolbarTitle)
    }

    fun updateNotificationDot(animate: Boolean) {
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
                    getString(viewModel.lastWatchExpiry.stringId)))
            if (!viewModel.watchlistExpiryChanged) {
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                    viewModel.watchlistExpiryChanged = true
                    bottomSheetPresenter.show(supportFragmentManager, WatchlistExpiryDialog.newInstance(viewModel.lastWatchExpiry))
                }
            }
            snackbar.show()
        }
    }

    private fun updateConcatAdapter() {
        if (actionMode == null) {
            concatAdapter.apply {
                addAdapter(0, headerAdapter)
                addAdapter(2, footerAdapter)
            }
        } else {
            concatAdapter.apply {
                removeAdapter(headerAdapter)
                removeAdapter(footerAdapter)
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
            binding.searchContainer.setCardBackgroundColor(ResourceUtil.getThemedColor(this@TalkTopicsActivity, R.attr.color_group_22))

            binding.searchContainer.setOnClickListener {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback)
                }
            }

            binding.talkSortButton.setOnClickListener {
                TalkTopicsSortOverflowView(this@TalkTopicsActivity).show(binding.talkSortButton, viewModel.currentSortMode, funnel) {
                    viewModel.currentSortMode = it
                    talkTopicItemAdapter.notifyDataSetChanged()
                }
            }

            FeedbackUtil.setButtonLongPressToast(binding.talkSortButton)
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

    private inner class FooterItemAdapter : RecyclerView.Adapter<FooterViewHolder>() {
        override fun onBindViewHolder(holder: FooterViewHolder, position: Int) { }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
            return FooterViewHolder(ViewTalkTopicsFooterBinding.inflate(layoutInflater, parent, false))
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class FooterViewHolder constructor(binding: ViewTalkTopicsFooterBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Update last modified date
            viewModel.lastRevision?.let { revision ->
                binding.lastModifiedText.text = StringUtil.fromHtml(getString(R.string.talk_footer_last_modified,
                    DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(revision.timeStamp).time,
                        System.currentTimeMillis(), 0L), revision.user))

                binding.viewEditHistoryContainer.setOnClickListener {
                    startActivity(ArticleEditDetailsActivity.newIntent(this@TalkTopicsActivity, viewModel.pageTitle, revision.revId))
                }
            }

            binding.viewPageContainer.setOnClickListener {
                goToPage()
            }

            if (viewModel.pageTitle.namespace() == Namespace.USER_TALK) {
                binding.viewPageIcon.setImageResource(R.drawable.ic_user_avatar)
                binding.viewPageTitle.text = getString(R.string.talk_footer_view_user_page)
            } else {
                binding.viewPageIcon.setImageResource(R.drawable.ic_article_ltr_ooui)
                binding.viewPageTitle.text = getString(R.string.talk_footer_view_article)
            }
            binding.viewPageContent.text = StringUtil.fromHtml(StringUtil.removeNamespace(viewModel.pageTitle.displayText))
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
            searchActionProvider = SearchActionProvider(this@TalkTopicsActivity, searchHintString,
                object : SearchActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }
                })

            val menuItem = menu.add(searchHintString)

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
        private const val EXTRA_PAGE_TITLE = "pageTitle"
        private const val EXTRA_GO_TO_TOPIC = "goToTopic"

        fun newIntent(context: Context, pageTitle: PageTitle, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                .putExtra(EXTRA_PAGE_TITLE, pageTitle)
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
