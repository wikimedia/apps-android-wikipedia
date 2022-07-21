package org.wikipedia.edit.insertmedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityInsertMediaBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.*
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PageItemView
import org.wikipedia.views.WikiErrorView

class InsertMediaActivity : BaseActivity() {
    private lateinit var binding: ActivityInsertMediaBinding

    private val insertMediaAdapter = ArchivedTalkPagesAdapter()
    private val insertMediaLoadHeader = LoadingItemAdapter { insertMediaAdapter.retry(); }
    private val insertMediaLoadFooter = LoadingItemAdapter { insertMediaAdapter.retry(); }
    private val insertMediaConcatAdapter = insertMediaAdapter.withLoadStateHeaderAndFooter(insertMediaLoadHeader, insertMediaLoadFooter)

    private val itemCallback = ItemCallback()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val viewModel: InsertMediaViewModel by viewModels { InsertMediaViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        binding.recyclerView.adapter = insertMediaConcatAdapter

        lifecycleScope.launch {
            viewModel.archivedTalkPagesFlow.collectLatest {
                insertMediaAdapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            insertMediaAdapter.loadStateFlow.collectLatest {
                insertMediaLoadHeader.loadState = it.refresh
                insertMediaLoadFooter.loadState = it.append
                val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && insertMediaAdapter.itemCount == 0)
                if (showEmpty) {
                    insertMediaConcatAdapter.addAdapter(EmptyItemAdapter(R.string.archive_empty))
                }
            }
        }
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
            val view = PageItemView<PageTitle>(this@InsertMediaActivity)
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
            // TODO: toggle
        }

        override fun onLongClick(item: PageTitle?): Boolean {
            return false
        }

        override fun onActionClick(item: PageTitle?, view: View) {}

        override fun onListChipClick(readingList: ReadingList) {}
    }

    companion object {
        const val EXTRA_TITLE = "talkTopicTitle"

        fun newIntent(context: Context, talkTopicTitle: PageTitle): Intent {
            return Intent(context, InsertMediaActivity::class.java)
                    .putExtra(EXTRA_TITLE, talkTopicTitle)
        }
    }
}
