package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.TalkFunnel
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTalkTopicBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.edit.EditHandler
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.util.*

class TalkTopicActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private lateinit var binding: ActivityTalkTopicBinding
    private lateinit var talkFunnel: TalkFunnel
    private lateinit var editFunnel: EditFunnel
    private lateinit var linkHandler: TalkLinkHandler

    private val viewModel: TalkTopicViewModel by viewModels { TalkTopicViewModel.Factory(intent.extras!!) }
    private val threadAdapter = TalkReplyItemAdapter()
    private val headerAdapter = HeaderItemAdapter()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    private val linkMovementMethod = LinkMovementMethodExt { url, title, linkText, x, y ->
        linkHandler.onUrlClick(url, title, linkText, x, y)
    }

    private val requestEditSource = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            // TODO: maybe add funnel?
            loadTopics()
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
        setSupportActionBar(binding.replyToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        linkHandler = TalkLinkHandler(this)
        linkHandler.wikiSite = viewModel.pageTitle.wikiSite

        L10nUtil.setConditionalLayoutDirection(binding.talkRefreshView, viewModel.pageTitle.wikiSite.languageCode)
        binding.talkRefreshView.setColorSchemeResources(ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent))

        binding.talkRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.talkErrorView.backClickListener = View.OnClickListener {
            finish()
        }
        binding.talkErrorView.retryClickListener = View.OnClickListener {
            loadTopics()
        }

        binding.talkRefreshView.setOnRefreshListener {
            talkFunnel.logRefresh()
            loadTopics()
        }

        talkFunnel = TalkFunnel(viewModel.pageTitle, intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource)
        talkFunnel.logOpenTopic()

        editFunnel = EditFunnel(WikipediaApp.getInstance(), viewModel.pageTitle)

        binding.talkRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                binding.talkToolbarSubjectView.isVisible = binding.talkRecyclerView.computeVerticalScrollOffset() > recyclerView.getChildAt(0).height
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
                        StringUtil.fromHtml(viewModel.topic!!.html).trim().ifEmpty { getString(R.string.talk_no_subject) }), FeedbackUtil.LENGTH_DEFAULT)
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_talk_topic, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            it.findItem(R.id.menu_edit_source)?.isVisible = AccountUtil.isLoggedIn
            if (viewModel.isExpandable) {
                val fullyExpanded = viewModel.isFullyExpanded
                it.findItem(R.id.menu_talk_topic_expand)?.isVisible = !fullyExpanded
                it.findItem(R.id.menu_talk_topic_collapse)?.isVisible = fullyExpanded
            } else {
                it.findItem(R.id.menu_talk_topic_expand)?.isVisible = false
                it.findItem(R.id.menu_talk_topic_collapse)?.isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_talk_topic_share -> {
                ShareUtil.shareText(this, getString(R.string.talk_share_discussion_subject, viewModel.topic?.html?.ifEmpty { getString(R.string.talk_no_subject) }), viewModel.pageTitle.uri + "#" + StringUtil.addUnderscores(viewModel.topic?.html))
                true
            }
            R.id.menu_edit_source -> {
                requestEditSource.launch(EditSectionActivity.newIntent(this, viewModel.sectionId, null, viewModel.pageTitle))
                true
            }
            R.id.menu_talk_topic_expand -> {
                viewModel.expandOrCollapseAll(true).dispatchUpdatesTo(threadAdapter)
                threadAdapter.notifyItemRangeChanged(0, threadAdapter.itemCount)
                invalidateOptionsMenu()
                true
            }
            R.id.menu_talk_topic_collapse -> {
                viewModel.expandOrCollapseAll(false).dispatchUpdatesTo(threadAdapter)
                threadAdapter.notifyItemRangeChanged(0, threadAdapter.itemCount)
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                        binding.talkRecyclerView.smoothScrollToPosition(position)
                        threadAdapter.notifyItemChanged(position)
                    }
                }
            }
        }
        binding.talkToolbarSubjectView.text = StringUtil.fromHtml(viewModel.topic?.html)
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
            view.bind(viewModel.pageTitle, viewModel.topic!!, viewModel.subscribed)
            view.callback = this
        }

        override fun onSubscribeClick() {
            viewModel.toggleSubscription()
        }
    }

    internal inner class TalkReplyHolder internal constructor(view: TalkThreadItemView) : RecyclerView.ViewHolder(view), TalkThreadItemView.Callback {
        fun bindItem(item: ThreadItem) {
            (itemView as TalkThreadItemView).let {
                it.bindItem(item, linkMovementMethod)
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
            UserTalkPopupHelper.show(this@TalkTopicActivity, bottomSheetPresenter,
                    PageTitle(UserAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode), item.author, viewModel.pageTitle.wikiSite),
                    !AccountUtil.isLoggedIn, view, Constants.InvokeSource.TALK_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
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
            // TODO
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // TODO
        }

        override lateinit var wikiSite: WikiSite

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            UserTalkPopupHelper.show(this@TalkTopicActivity, bottomSheetPresenter, title, false, lastX, lastY,
                    Constants.InvokeSource.TALK_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    private fun startReplyActivity(item: ThreadItem, undoSubject: CharSequence? = null, undoBody: CharSequence? = null) {
        talkFunnel.logReplyClick()
        replyResult.launch(TalkReplyActivity.newIntent(this@TalkTopicActivity, viewModel.pageTitle,
                item, Constants.InvokeSource.TALK_ACTIVITY, undoSubject, undoBody))
    }

    private fun onSaveError(t: Throwable) {
        editFunnel.logError(t.message)
        EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle)
        binding.talkProgressBar.visibility = View.GONE
        FeedbackUtil.showError(this, t)
    }

    private fun showUndoSnackbar(undoRevId: Long) {
        FeedbackUtil.makeSnackbar(this, getString(R.string.talk_response_submitted), FeedbackUtil.LENGTH_DEFAULT)
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
        ClipboardUtil.setPlainText(this, null, title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    companion object {
        const val EXTRA_PAGE_TITLE = "pageTitle"
        const val EXTRA_TOPIC = "topicName"
        const val RESULT_NEW_REVISION_ID = "newRevisionId"

        fun newIntent(context: Context,
                      pageTitle: PageTitle,
                      topicId: String,
                      invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, TalkTopicActivity::class.java)
                    .putExtra(EXTRA_PAGE_TITLE, pageTitle)
                    .putExtra(EXTRA_TOPIC, topicId)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
