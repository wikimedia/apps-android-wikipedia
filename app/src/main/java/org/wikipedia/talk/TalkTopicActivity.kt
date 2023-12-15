package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.location.Location
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
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityTalkTopicBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.edit.EditHandler
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.SearchActionModeCallback
import org.wikipedia.login.LoginActivity
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.places.PlacesActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.util.*
import org.wikipedia.views.SearchActionProvider
import org.wikipedia.views.ViewUtil

class TalkTopicActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private lateinit var binding: ActivityTalkTopicBinding
    private lateinit var linkHandler: TalkLinkHandler

    private val viewModel: TalkTopicViewModel by viewModels { TalkTopicViewModel.Factory(intent.extras!!) }
    private val threadAdapter = TalkReplyItemAdapter()
    private val headerAdapter = HeaderItemAdapter()
    private var actionMode: ActionMode? = null
    private val searchActionModeCallback = SearchCallback()

    private val linkMovementMethod = LinkMovementMethodExt { url, title, linkText, x, y ->
        linkHandler.onUrlClick(url, title, linkText, x, y)
    }

    private val requestEditSource = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            loadTopics()
        }
    }

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            viewModel.toggleSubscription()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        }
    }

    private val replyResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS) {
            result.data?.let {
                viewModel.undoSubject = it.getCharSequenceExtra(TalkReplyActivity.EXTRA_SUBJECT)
                viewModel.undoBody = it.getCharSequenceExtra(TalkReplyActivity.EXTRA_BODY)
                viewModel.undoTopicId = it.getStringExtra(TalkReplyActivity.EXTRA_TOPIC_ID)
                val undoRevId = it.getLongExtra(TalkReplyActivity.RESULT_NEW_REVISION_ID, -1)
                if (undoRevId >= 0) {
                    showUndoSnackbar(undoRevId)
                }
            }
            loadTopics()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkTopicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        linkHandler = TalkLinkHandler(this)
        linkHandler.wikiSite = viewModel.pageTitle.wikiSite

        L10nUtil.setConditionalLayoutDirection(binding.talkRecyclerView, viewModel.pageTitle.wikiSite.languageCode)
        L10nUtil.setConditionalLayoutDirection(binding.talkErrorView, viewModel.pageTitle.wikiSite.languageCode)
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.progressive_color))

        ViewUtil.getTitleViewFromToolbar(binding.toolbar)?.let {
            it.movementMethod = linkMovementMethod
        }

        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.talkErrorView.backClickListener = View.OnClickListener {
            finish()
        }
        binding.talkErrorView.retryClickListener = View.OnClickListener {
            loadTopics()
        }

        binding.talkRefreshView.setOnRefreshListener {
            loadTopics()
        }

        binding.talkRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                supportActionBar?.setDisplayShowTitleEnabled(binding.talkRecyclerView.computeVerticalScrollOffset() > (recyclerView.getChildAt(0).height / 2))
            }
        })

        viewModel.threadItemsData.observe(this) {
            when (it) {
                is Resource.Success -> updateOnSuccess(it.data)
                is Resource.Error -> updateOnError(it.throwable)
            }
        }

        viewModel.subscribeData.observe(this) {
            if (it is Resource.Success) {
                FeedbackUtil.showMessage(this, getString(if (it.data) R.string.talk_thread_subscribed_to else R.string.talk_thread_unsubscribed_from,
                        StringUtil.fromHtml(viewModel.topic!!.html).trim().ifEmpty { getString(R.string.talk_no_subject) }))
                headerAdapter.notifyItemChanged(0)
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(this, it.throwable)
            }
        }

        viewModel.undoResponseData.observe(this) {
            if (it is Resource.Success) {
                binding.talkProgressBar.isVisible = false
                viewModel.findTopicById(viewModel.undoTopicId)?.let { item ->
                    startReplyActivity(item, viewModel.undoSubject, viewModel.undoBody)
                }
                loadTopics()
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(this, it.throwable)
            }
        }

        onInitialLoad()
        viewModel.currentSearchQuery?.let {
            showFindInPage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_talk_topic, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_find_in_page)?.isVisible = viewModel.topic?.replies.orEmpty().isNotEmpty()
        menu.findItem(R.id.menu_edit_source)?.isVisible = AccountUtil.isLoggedIn
        if (viewModel.isExpandable) {
            val fullyExpanded = viewModel.isFullyExpanded
            menu.findItem(R.id.menu_talk_topic_expand)?.isVisible = !fullyExpanded
            menu.findItem(R.id.menu_talk_topic_collapse)?.isVisible = fullyExpanded
        } else {
            menu.findItem(R.id.menu_talk_topic_expand)?.isVisible = false
            menu.findItem(R.id.menu_talk_topic_collapse)?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_discussion_subject, viewModel.topic?.html?.ifEmpty { getString(R.string.talk_no_subject) }), viewModel.pageTitle.uri + "#" + UriUtil.encodeURL(StringUtil.addUnderscores(StringUtil.fromHtml(viewModel.topic?.html).toString())))
                true
            }
            R.id.menu_edit_source -> {
                requestEditSource.launch(EditSectionActivity.newIntent(this, viewModel.sectionId, null,
                    viewModel.pageTitle, Constants.InvokeSource.TALK_TOPIC_ACTIVITY))
                true
            }
            R.id.menu_talk_topic_expand -> {
                expandOrCollapseAll(true)
                true
            }
            R.id.menu_talk_topic_collapse -> {
                expandOrCollapseAll(false)
                true
            }
            R.id.menu_find_in_page -> {
                expandOrCollapseAll(true)
                showFindInPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun expandOrCollapseAll(expand: Boolean) {
        Prefs.talkTopicExpandOrCollapseByDefault = expand
        viewModel.expandOrCollapseAll().dispatchUpdatesTo(threadAdapter)
        threadAdapter.notifyItemRangeChanged(0, threadAdapter.itemCount)
        invalidateOptionsMenu()
    }

    private fun showFindInPage() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(searchActionModeCallback)
        }
    }

    private inner class SearchCallback : SearchActionModeCallback() {
        var searchActionProvider: SearchActionProvider? = null
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            searchActionProvider = SearchActionProvider(this@TalkTopicActivity, searchHintString,
                object : SearchActionProvider.Callback {
                    override fun onQueryTextChange(s: String) {
                        onQueryChange(s)
                    }

                    override fun onQueryTextFocusChange() {
                    }
                })

            val menuItem = menu.add(searchHintString)

            MenuItemCompat.setActionProvider(menuItem, searchActionProvider)
            searchActionProvider?.setQueryText(viewModel.currentSearchQuery)
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
            return super.onCreateActionMode(mode, menu)
        }

        override fun onQueryChange(s: String) {
            viewModel.currentSearchQuery = s
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            actionMode = null
            viewModel.currentSearchQuery = null
            binding.talkRecyclerView.adapter?.notifyDataSetChanged()
        }

        override fun getSearchHintString(): String {
            return getString(R.string.talk_search_find_in_talk_topics_hint)
        }

        override fun getParentContext(): Context {
            return this@TalkTopicActivity
        }
    }

    private fun onInitialLoad() {
        binding.talkProgressBar.visibility = View.VISIBLE
        binding.talkErrorView.visibility = View.GONE
        DeviceUtil.hideSoftKeyboard(this)
    }

    private fun loadTopics() {
        binding.talkProgressBar.visibility = View.VISIBLE
        binding.talkErrorView.visibility = View.GONE
        viewModel.loadTopic()
    }

    private fun updateOnSuccess(threadItems: List<ThreadItem>) {
        binding.talkProgressBar.visibility = View.GONE
        binding.talkRefreshView.isRefreshing = false

        if (binding.talkRecyclerView.adapter == null) {
            binding.talkRecyclerView.adapter = ConcatAdapter().apply {
                addAdapter(headerAdapter)
                addAdapter(threadAdapter)
            }
        } else {
            headerAdapter.notifyItemChanged(0)
            threadAdapter.notifyDataSetChanged()
        }
        if (!viewModel.scrollTargetId.isNullOrEmpty()) {
            val position = 1 + viewModel.flattenedThreadItems.indexOfFirst { it.id == viewModel.scrollTargetId }
            if (position >= 0) {
                binding.talkRecyclerView.post {
                    if (!isDestroyed) {
                        ViewUtil.jumpToPositionWithoutAnimation(binding.talkRecyclerView, position)
                        threadAdapter.notifyItemChanged(position)
                    }
                }
            }
        }
        supportActionBar?.title = StringUtil.fromHtml(viewModel.topic?.html)
        invalidateOptionsMenu()
    }

    private fun updateOnError(t: Throwable) {
        binding.talkProgressBar.visibility = View.GONE
        binding.talkRefreshView.isRefreshing = false
        binding.talkErrorView.visibility = View.VISIBLE
        binding.talkErrorView.setError(t)
    }

    private inner class HeaderItemAdapter : RecyclerView.Adapter<HeaderViewHolder>() {
        override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
            holder.bindItem()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
            return HeaderViewHolder(TalkThreadHeaderView(this@TalkTopicActivity))
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class HeaderViewHolder constructor(private val view: TalkThreadHeaderView) : RecyclerView.ViewHolder(view), TalkThreadHeaderView.Callback {
        fun bindItem() {
            view.bind(viewModel.pageTitle, viewModel.topic, viewModel.subscribed, linkMovementMethod, viewModel.currentSearchQuery)
            view.callback = this
        }

        override fun onSubscribeClick() {
            if (AccountUtil.isLoggedIn) {
                viewModel.toggleSubscription()
            } else {
                MaterialAlertDialogBuilder(this@TalkTopicActivity)
                    .setTitle(R.string.talk_login_to_subscribe_dialog_title)
                    .setMessage(R.string.talk_login_to_subscribe_dialog_content)
                    .setPositiveButton(R.string.login_join_wikipedia) { _, _ ->
                        requestLogin.launch(LoginActivity.newIntent(this@TalkTopicActivity, LoginActivity.SOURCE_SUBSCRIBE))
                    }
                    .setNegativeButton(R.string.onboarding_maybe_later, null)
                    .show()
            }
        }
    }

    internal inner class TalkReplyHolder internal constructor(view: TalkThreadItemView) : RecyclerView.ViewHolder(view), TalkThreadItemView.Callback {
        fun bindItem(item: ThreadItem) {
            (itemView as TalkThreadItemView).let {
                it.bindItem(item, linkMovementMethod, searchQuery = viewModel.currentSearchQuery)
                if (item.id == viewModel.scrollTargetId) {
                    viewModel.scrollTargetId = null
                    it.animateSelectedBackground()
                }
                it.callback = this
            }
        }

        override fun onExpandClick(item: ThreadItem) {
            viewModel.toggleItemExpanded(item).dispatchUpdatesTo(threadAdapter)
            invalidateOptionsMenu()
        }

        override fun onReplyClick(item: ThreadItem) {
            startReplyActivity(item)
        }

        override fun onShareClick(item: ThreadItem) {
            val title = viewModel.pageTitle.copy()
            title.fragment = item.id
            ShareUtil.shareText(this@TalkTopicActivity, title)
        }

        override fun onUserNameClick(item: ThreadItem, view: View) {
            UserTalkPopupHelper.show(this@TalkTopicActivity,
                    PageTitle(UserAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode), item.author, viewModel.pageTitle.wikiSite),
                    !AccountUtil.isLoggedIn, view, Constants.InvokeSource.TALK_TOPIC_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    internal inner class TalkReplyItemAdapter : RecyclerView.Adapter<TalkReplyHolder>() {
        override fun getItemCount(): Int {
            return viewModel.flattenedThreadItems.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): TalkReplyHolder {
            return TalkReplyHolder(TalkThreadItemView(parent.context))
        }

        override fun onBindViewHolder(holder: TalkReplyHolder, pos: Int) {
            holder.bindItem(viewModel.flattenedThreadItems[pos])
        }
    }

    internal inner class TalkLinkHandler internal constructor(context: Context) : LinkHandler(context) {
        private var lastX: Int = 0
        private var lastY: Int = 0

        fun onUrlClick(url: String, title: String?, linkText: String, x: Int, y: Int) {
            lastX = x
            lastY = y
            super.onUrlClick(url, title, linkText)
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            startActivity(FilePageActivity.newIntent(this@TalkTopicActivity, title))
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // TODO
        }

        override lateinit var wikiSite: WikiSite

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            UserTalkPopupHelper.show(this@TalkTopicActivity, title, false, lastX, lastY,
                    Constants.InvokeSource.TALK_TOPICS_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    private fun startReplyActivity(item: ThreadItem, undoSubject: CharSequence? = null, undoBody: CharSequence? = null) {
        replyResult.launch(TalkReplyActivity.newIntent(this@TalkTopicActivity, viewModel.pageTitle, viewModel.topic?.html,
                item, Constants.InvokeSource.TALK_TOPIC_ACTIVITY, undoSubject, undoBody))
    }

    private fun showUndoSnackbar(undoRevId: Long) {
        FeedbackUtil.makeSnackbar(this, getString(R.string.talk_response_submitted))
                .setAction(R.string.talk_snackbar_undo) {
                    binding.talkProgressBar.visibility = View.VISIBLE
                    viewModel.undo(undoRevId)
                }
                .show()
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, title) else
            PageActivity.newIntentForCurrentTab(this, entry, title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, text = title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_TOPIC_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    override fun onLinkPreviewViewOnMap(title: PageTitle, location: Location?) {
        startActivity(PlacesActivity.newIntent(this, title.wikiSite, title, location))
    }

    companion object {
        const val EXTRA_TOPIC_NAME = "topicName"
        const val EXTRA_TOPIC_ID = "topicId"
        const val EXTRA_REPLY_ID = "replyId"
        const val EXTRA_SEARCH_QUERY = "searchQuery"

        fun newIntent(context: Context,
                      pageTitle: PageTitle,
                      topicName: String,
                      topicId: String,
                      replyId: String?,
                      searchQuery: String?,
                      invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, pageTitle)
                    .putExtra(EXTRA_TOPIC_NAME, topicName)
                    .putExtra(EXTRA_TOPIC_ID, topicId)
                    .putExtra(EXTRA_REPLY_ID, replyId)
                    .putExtra(EXTRA_SEARCH_QUERY, searchQuery)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }

        fun isSubscribable(item: ThreadItem?): Boolean {
            return item?.name.orEmpty().length > 2
        }

        fun isHeaderTemplate(item: ThreadItem?): Boolean {
            return item?.headingLevel == 0 && item.author.isEmpty() && item.html.isEmpty()
        }
    }
}
