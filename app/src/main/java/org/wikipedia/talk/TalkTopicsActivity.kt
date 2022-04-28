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
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
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
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.util.*
import org.wikipedia.views.*

class TalkTopicsActivity : BaseActivity() {
    private lateinit var binding: ActivityTalkTopicsBinding
    private lateinit var pageTitle: PageTitle
    private lateinit var invokeSource: Constants.InvokeSource
    private lateinit var notificationButtonView: NotificationButtonView
    private val viewModel: TalkTopicsViewModel by viewModels { TalkTopicsViewModel.Factory(intent.getParcelableExtra(EXTRA_PAGE_TITLE)) }
    private var funnel: TalkFunnel? = null
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()
    private var revisionForLastEdit: MwQueryPage.Revision? = null
    private var goToTopic = false

    private val requestLanguageChange = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.let { intent ->
                if (intent.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                    val pos = intent.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
                    if (pos < WikipediaApp.getInstance().language().appLanguageCodes.size) {
                        funnel?.logChangeLanguage()

                        val newNamespace = when {
                            pageTitle.namespace() == Namespace.USER -> {
                                UserAliasData.valueFor(WikipediaApp.getInstance().language().appLanguageCodes[pos])
                            }
                            pageTitle.namespace() == Namespace.USER_TALK -> {
                                UserTalkAliasData.valueFor(WikipediaApp.getInstance().language().appLanguageCodes[pos])
                            }
                            else -> pageTitle.namespace
                        }

                        pageTitle = PageTitle(newNamespace, StringUtil.removeNamespace(pageTitle.prefixedText),
                            WikiSite.forLanguageCode(WikipediaApp.getInstance().language().appLanguageCodes[pos]))

                        resetViews()
                        viewModel.pageTitle = pageTitle
                        viewModel.loadTopics()
                    }
                }
            }
        }
    }

    private val requestNewTopic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == TalkTopicActivity.RESULT_EDIT_SUCCESS) {
            val newRevisionId = it.data?.getLongExtra(TalkTopicActivity.RESULT_NEW_REVISION_ID, 0) ?: 0
            // TODO: fix this
            val topic = it.data?.getStringExtra(TalkTopicActivity.EXTRA_TOPIC) ?: ""
            val undoneSubject = it.data?.getStringExtra(TalkTopicActivity.EXTRA_SUBJECT) ?: ""
            val undoneText = it.data?.getStringExtra(TalkTopicActivity.EXTRA_BODY) ?: ""
            if (newRevisionId > 0) {
                FeedbackUtil.makeSnackbar(this, getString(R.string.talk_new_topic_submitted), FeedbackUtil.LENGTH_DEFAULT)
                    .setAnchorView(binding.talkNewTopicButton)
                    .setAction(R.string.talk_snackbar_undo) {
                        binding.talkNewTopicButton.isEnabled = false
                        binding.talkNewTopicButton.alpha = 0.5f
                        binding.talkProgressBar.visibility = View.VISIBLE
                        viewModel.undoSave(newRevisionId, topic, undoneSubject, undoneText)
                    }
                    .show()
            }
        }
    }

    private val requestGoToTopic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == TalkTopicActivity.RESULT_BACK_FROM_TOPIC) {
            finish()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        goToTopic = intent.getBooleanExtra(EXTRA_GO_TO_TOPIC, false)
        pageTitle = intent.getParcelableExtra(EXTRA_PAGE_TITLE)!!
        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.talkRecyclerView.addItemDecoration(FooterMarginItemDecoration(0, 120))
        binding.talkRecyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false, skipSearchBar = true))
        binding.talkRecyclerView.adapter = TalkTopicItemAdapter()

        binding.talkErrorView.backClickListener = View.OnClickListener {
            finish()
        }
        binding.talkErrorView.retryClickListener = View.OnClickListener {
            resetViews()
            viewModel.loadTopics()
        }

        binding.talkNewTopicButton.setOnClickListener {
            funnel?.logNewTopicClick()
            requestNewTopic.launch(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, NEW_TOPIC_ID, invokeSource))
        }

        binding.talkRefreshView.setOnRefreshListener {
            funnel?.logRefresh()
            resetViews()
            viewModel.loadTopics()
        }
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent))

        invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource

        binding.talkNewTopicButton.visibility = View.GONE

        binding.talkLastModified.visibility = View.GONE
        binding.talkLastModified.setOnClickListener {
            revisionForLastEdit?.let {
                startActivity(ArticleEditDetailsActivity.newIntent(this, pageTitle, it.revId))
            }
        }
        notificationButtonView = NotificationButtonView(this)
        Prefs.hasAnonymousNotification = false

        lifecycleScope.launchWhenCreated {
            viewModel.uiState.collect {
                when (it) {
                    is TalkTopicsViewModel.UiState.LoadTopic -> updateOnSuccess(it.pageTitle, it.threadItems, it.lastModifiedResponse)
                    is TalkTopicsViewModel.UiState.UndoEdit -> updateOnUndoSave(it.topicId, it.undoneSubject, it.undoneBody)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!goToTopic) {
            menuInflater.inflate(R.menu.menu_talk, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (!goToTopic) {
            menu!!.findItem(R.id.menu_change_language).isVisible = pageTitle.namespace() == Namespace.USER_TALK
            menu.findItem(R.id.menu_view_user_page).isVisible = pageTitle.namespace() == Namespace.USER_TALK
            val notificationMenuItem = menu.findItem(R.id.menu_notifications)
            if (AccountUtil.isLoggedIn) {
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
            } else {
                notificationMenuItem.isVisible = false
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
            R.id.menu_view_in_browser -> {
                UriUtil.visitInExternalBrowser(this, Uri.parse(pageTitle.uri))
                return true
            }
            R.id.menu_view_user_page -> {
                val entry = HistoryEntry(PageTitle(UserAliasData.valueFor(pageTitle.wikiSite.languageCode) + ":" + pageTitle.text, pageTitle.wikiSite), HistoryEntry.SOURCE_TALK_TOPIC)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
                return true
            }
            R.id.menu_view_edit_history -> {
                startActivity(EditHistoryListActivity.newIntent(this, pageTitle))
                return true
            }
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_talk_page), pageTitle.uri)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onUnreadNotification() {
        updateNotificationDot(true)
    }

    private fun resetViews() {
        invalidateOptionsMenu()
        L10nUtil.setConditionalLayoutDirection(binding.talkRefreshView, pageTitle.wikiSite.languageCode)
        binding.toolbarTitle.text = StringUtil.fromHtml(pageTitle.displayText)
        binding.toolbarTitle.contentDescription = binding.toolbarTitle.text
        binding.toolbarTitle.isVisible = !goToTopic
        FeedbackUtil.setButtonLongPressToast(binding.toolbarTitle)

        binding.talkProgressBar.isVisible = true
        binding.talkErrorView.visibility = View.GONE
        binding.talkEmptyContainer.visibility = View.GONE
    }

    private fun updateOnSuccess(pageTitle: PageTitle, threadItems: List<ThreadItem>, lastModifiedResponse: MwQueryResponse) {
        // Update page title and start the funnel
        this.pageTitle = pageTitle
        funnel = TalkFunnel(pageTitle, invokeSource)
        binding.toolbarTitle.text = StringUtil.fromHtml(pageTitle.displayText)

        // Update last modified date
        lastModifiedResponse.query?.firstPage()?.revisions?.firstOrNull()?.let { revision ->
            revisionForLastEdit = revision
            binding.talkLastModified.text = StringUtil.fromHtml(getString(R.string.talk_last_modified,
                DateUtils.getRelativeTimeSpanString(DateUtil.iso8601DateParse(revision.timeStamp).time,
                    System.currentTimeMillis(), 0L), revision.user))
        }

        if (intent.getBooleanExtra(EXTRA_GO_TO_TOPIC, false)) {
            intent.putExtra(EXTRA_GO_TO_TOPIC, false)
            var threadItem: ThreadItem? = null
            if (!pageTitle.fragment.isNullOrEmpty()) {
                val targetTopic = UriUtil.parseTalkTopicFromFragment(pageTitle.fragment.orEmpty())
                threadItem = threadItems.find {
                    StringUtil.addUnderscores(targetTopic) == StringUtil.addUnderscores(it.html)
                }
            }
            if (threadItem != null) {
                requestGoToTopic.launch(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, threadItem.name, invokeSource))
                overridePendingTransition(0, 0)
                return
            }
        }
        goToTopic = false
        if (threadItems.isEmpty()) {
            updateOnEmpty()
        } else {
            binding.talkErrorView.visibility = View.GONE
            if (actionMode != null) {
                binding.talkNewTopicButton.hide()
                binding.talkLastModified.visibility = View.GONE
            } else {
                binding.talkNewTopicButton.show()
                binding.talkNewTopicButton.isEnabled = true
                binding.talkNewTopicButton.alpha = 1.0f
                binding.talkLastModified.visibility = View.VISIBLE
            }
            binding.tabLayout.visibility = if (actionMode != null) View.GONE else View.VISIBLE
            binding.talkRecyclerView.visibility = View.VISIBLE
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        }
        funnel?.logOpenTalk()

        invalidateOptionsMenu()
        binding.toolbarTitle.isVisible = !goToTopic
        binding.talkProgressBar.isVisible = !goToTopic
        binding.talkProgressBar.visibility = View.GONE
        binding.talkRefreshView.isRefreshing = false
    }

    private fun updateOnError(t: Throwable) {
        binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        binding.talkRecyclerView.visibility = View.GONE

        // In the case of 404, it just means that the talk page hasn't been created yet.
        if (t is HttpStatusException && t.code == 404) {
            updateOnEmpty()
            invalidateOptionsMenu()
        } else {
            binding.talkNewTopicButton.hide()
            binding.talkLastModified.visibility = View.GONE
            binding.talkErrorView.visibility = View.VISIBLE
            binding.talkErrorView.setError(t)
        }
    }

    private fun updateOnEmpty() {
        binding.talkRecyclerView.visibility = View.GONE
        binding.talkEmptyContainer.visibility = View.VISIBLE
        binding.talkLastModified.visibility = View.GONE
        // Allow them to create a new topic anyway
        binding.talkNewTopicButton.show()
    }

    private fun updateOnUndoSave(topicId: String, undoneSubject: String, undoneBody: String) {
        // TODO: discuss this
        startActivity(TalkTopicActivity.newIntent(this@TalkTopicsActivity, pageTitle, topicId, invokeSource, undoneSubject, undoneBody))
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

    internal inner class TalkTopicItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val listPlaceholder get() = if (actionMode == null) 1 else 0

        override fun getItemCount(): Int {
            return viewModel.sortedThreadItems.size + listPlaceholder
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0 && listPlaceholder == 1) ITEM_SEARCH_BAR else ITEM_TOPIC
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            if (type == ITEM_SEARCH_BAR) {
                return TalkTopicSearcherHolder(layoutInflater.inflate(R.layout.view_talk_topic_search_bar, parent, false))
            }
            return TalkTopicHolder(ItemTalkTopicBinding.inflate(layoutInflater, parent, false), this@TalkTopicsActivity, pageTitle, invokeSource)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            if (holder is TalkTopicHolder) {
                holder.bindItem(viewModel.sortedThreadItems[pos - listPlaceholder], viewModel.currentSearchQuery)
            }
        }
    }

    private inner class TalkTopicSearcherHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val talkSortButton: AppCompatImageView = itemView.findViewById(R.id.talk_sort_button)

        init {
            (itemView as WikiCardView).setCardBackgroundColor(ResourceUtil.getThemedColor(this@TalkTopicsActivity, R.attr.color_group_22))

            itemView.setOnClickListener {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(searchActionModeCallback)
                }
            }

            talkSortButton.setOnClickListener {
                TalkTopicsSortOverflowView(this@TalkTopicsActivity).show(talkSortButton, viewModel.currentSortMode) {
                    viewModel.currentSortMode = it
                    Prefs.talkTopicsSortMode = it
                    binding.talkRecyclerView.adapter?.notifyDataSetChanged()
                }
            }

            FeedbackUtil.setButtonLongPressToast(talkSortButton)
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
            binding.talkLastModified.isVisible = false
            binding.talkNewTopicButton.hide()
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.currentSearchQuery = s
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
            binding.talkSearchNoResult.isVisible = binding.talkRecyclerView.adapter?.itemCount == 0
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.currentSearchQuery = null
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
            binding.talkNewTopicButton.show()
            binding.talkLastModified.isVisible = true
            binding.talkSearchNoResult.isVisible = false
        }

        override fun getSearchHintString(): String {
            return getString(R.string.talk_search_hint)
        }

        override fun getParentContext(): Context {
            return this@TalkTopicsActivity
        }
    }

    companion object {
        private const val ITEM_SEARCH_BAR = 0
        private const val ITEM_TOPIC = 1
        private const val EXTRA_PAGE_TITLE = "pageTitle"
        private const val EXTRA_GO_TO_TOPIC = "goToTopic"
        // TODO: fix this
        const val NEW_TOPIC_ID = ""

        fun newIntent(context: Context, pageTitle: PageTitle, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicsActivity::class.java)
                .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                .putExtra(EXTRA_GO_TO_TOPIC, !pageTitle.fragment.isNullOrEmpty())
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
