package org.wikipedia.talk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityArchivedTalkPagesBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.*
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PageItemView
import org.wikipedia.views.ViewUtil
import org.wikipedia.views.WikiErrorView

class ArchivedTalkPagesActivity : BaseActivity() {
    private lateinit var binding: ActivityArchivedTalkPagesBinding

    private val archivedTalkPagesAdapter = ArchivedTalkPagesAdapter()
    private val archivedTalkPagesLoadHeader = LoadingItemAdapter { archivedTalkPagesAdapter.retry(); }
    private val archivedTalkPagesLoadFooter = LoadingItemAdapter { archivedTalkPagesAdapter.retry(); }
    private val archivedTalkPagesConcatAdapter = archivedTalkPagesAdapter.withLoadStateHeaderAndFooter(archivedTalkPagesLoadHeader, archivedTalkPagesLoadFooter)

    private val itemCallback = ItemCallback()
    private val viewModel: ArchivedTalkPagesViewModel by viewModels { ArchivedTalkPagesViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchivedTalkPagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setToolbarTitle(viewModel.pageTitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_divider, drawStart = false, drawEnd = false))
        binding.recyclerView.adapter = archivedTalkPagesConcatAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.archivedTalkPagesFlow.collectLatest {
                        archivedTalkPagesAdapter.submitData(it)
                    }
                }
                launch {
                    archivedTalkPagesAdapter.loadStateFlow.collectLatest {
                        archivedTalkPagesLoadHeader.loadState = it.refresh
                        archivedTalkPagesLoadFooter.loadState = it.append
                        val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && archivedTalkPagesAdapter.itemCount == 0)
                        if (showEmpty) {
                            archivedTalkPagesConcatAdapter.addAdapter(EmptyItemAdapter(R.string.archive_empty))
                        }
                    }
                }
            }
        }
    }

    private fun setToolbarTitle(pageTitle: PageTitle) {
        val title = StringUtil.fromHtml(getString(R.string.talk_archived_title, "<a href='#'>${StringUtil.removeNamespace(pageTitle.displayText)}</a>"))
        ViewUtil.getTitleViewFromToolbar(binding.toolbar)?.let {
            it.contentDescription = title
            it.movementMethod = LinkMovementMethodExt { _ ->
                val entry = HistoryEntry(TalkTopicsActivity.getNonTalkPageTitle(viewModel.pageTitle), HistoryEntry.SOURCE_ARCHIVED_TALK)
                startActivity(PageActivity.newIntentForNewTab(this, entry, entry.title))
            }
            FeedbackUtil.setButtonLongPressToast(it)
        }
        supportActionBar?.title = title
    }

    private inner class LoadingItemAdapter(private val retry: () -> Unit) : LoadStateAdapter<LoadingViewHolder>() {
        override fun onBindViewHolder(holder: LoadingViewHolder, loadState: LoadState) {
            holder.bindItem(loadState, retry)
        }

        override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadingViewHolder {
            return LoadingViewHolder(layoutInflater.inflate(R.layout.item_list_progress, parent, false))
        }
    }

    private inner class EmptyItemAdapter(@StringRes private val text: Int) : RecyclerView.Adapter<EmptyViewHolder>() {
        override fun onBindViewHolder(holder: EmptyViewHolder, position: Int) {
            holder.bindItem(text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyViewHolder {
            return EmptyViewHolder(layoutInflater.inflate(R.layout.item_list_progress, parent, false))
        }

        override fun getItemCount(): Int { return 1 }
    }

    private inner class ArchivedTalkPagesDiffCallback : DiffUtil.ItemCallback<PageTitle>() {
        override fun areItemsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return oldItem.prefixedText == newItem.prefixedText && oldItem.namespace == newItem.namespace
        }

        override fun areContentsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private inner class ArchivedTalkPagesAdapter : PagingDataAdapter<PageTitle, RecyclerView.ViewHolder>(ArchivedTalkPagesDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ArchivedTalkPageItemHolder {
            val view = PageItemView<PageTitle>(this@ArchivedTalkPagesActivity)
            view.callback = itemCallback
            return ArchivedTalkPageItemHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            getItem(position)?.let {
                (holder as ArchivedTalkPageItemHolder).bindItem(it)
            }
        }
    }

    private inner class LoadingViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(loadState: LoadState, retry: () -> Unit) {
            val errorView = itemView.findViewById<WikiErrorView>(R.id.errorView)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            progressBar.isVisible = loadState is LoadState.Loading
            errorView.isVisible = loadState is LoadState.Error
            errorView.retryClickListener = View.OnClickListener { retry() }
            if (loadState is LoadState.Error) {
                errorView.setError(loadState.error, viewModel.pageTitle)
            }
        }
    }

    private inner class EmptyViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(@StringRes text: Int) {
            val errorView = itemView.findViewById<WikiErrorView>(R.id.errorView)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            val emptyMessage = itemView.findViewById<TextView>(R.id.emptyMessage)
            progressBar.isVisible = false
            errorView.isVisible = false
            emptyMessage.text = getString(text)
            emptyMessage.isVisible = true
        }
    }

    private inner class ArchivedTalkPageItemHolder constructor(val view: PageItemView<PageTitle>) : RecyclerView.ViewHolder(view) {
        fun bindItem(title: PageTitle) {
            view.item = title
            view.setTitle(title.displayText)
            view.setImageUrl(title.thumbUrl)
            view.setImageVisible(!title.thumbUrl.isNullOrEmpty())
            view.setDescription(title.description)
        }
    }

    private inner class ItemCallback : PageItemView.Callback<PageTitle?> {
        override fun onClick(item: PageTitle?) {
            item?.let {
                startActivity(TalkTopicsActivity.newIntent(this@ArchivedTalkPagesActivity, it, InvokeSource.ARCHIVED_TALK_ACTIVITY))
            }
        }

        override fun onLongClick(item: PageTitle?): Boolean {
            return false
        }

        override fun onActionClick(item: PageTitle?, view: View) {}

        override fun onListChipClick(readingList: ReadingList) {}
    }

    companion object {
        fun newIntent(context: Context, talkTopicTitle: PageTitle): Intent {
            return Intent(context, ArchivedTalkPagesActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, talkTopicTitle)
        }
    }
}
