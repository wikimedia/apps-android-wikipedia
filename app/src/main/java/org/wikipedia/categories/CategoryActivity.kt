package org.wikipedia.categories

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
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityCategoryBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.*
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PageItemView
import org.wikipedia.views.WikiErrorView

class CategoryActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private lateinit var binding: ActivityCategoryBinding

    private val categoryMembersAdapter = CategoryMembersAdapter()
    private val categoryMembersLoadHeader = LoadingItemAdapter { categoryMembersAdapter.retry(); }
    private val categoryMembersLoadFooter = LoadingItemAdapter { categoryMembersAdapter.retry(); }
    private val categoryMembersConcatAdapter = categoryMembersAdapter.withLoadStateHeaderAndFooter(categoryMembersLoadHeader, categoryMembersLoadFooter)
    private val subcategoriesAdapter = CategoryMembersAdapter()
    private val subcategoriesLoadHeader = LoadingItemAdapter { subcategoriesAdapter.retry() }
    private val subcategoriesLoadFooter = LoadingItemAdapter { subcategoriesAdapter.retry() }
    private val subcategoriesConcatAdapter = subcategoriesAdapter.withLoadStateHeaderAndFooter(subcategoriesLoadHeader, subcategoriesLoadFooter)

    private val itemCallback = ItemCallback()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val viewModel: CategoryActivityViewModel by viewModels { CategoryActivityViewModel.Factory(intent.extras!!) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = viewModel.pageTitle.displayText

        binding.categoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.categoryRecycler.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        binding.categoryRecycler.adapter = categoryMembersConcatAdapter

        lifecycleScope.launch {
            viewModel.categoryMembersFlow.collectLatest {
                categoryMembersAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            viewModel.subcategoriesFlow.collectLatest {
                subcategoriesAdapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            categoryMembersAdapter.loadStateFlow.collectLatest {
                categoryMembersLoadHeader.loadState = it.refresh
                categoryMembersLoadFooter.loadState = it.append
                val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && categoryMembersAdapter.itemCount == 0)
                if (showEmpty) {
                    categoryMembersConcatAdapter.addAdapter(EmptyItemAdapter(R.string.category_empty))
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            subcategoriesAdapter.loadStateFlow.collectLatest {
                subcategoriesLoadHeader.loadState = it.refresh
                subcategoriesLoadFooter.loadState = it.append
                val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && subcategoriesAdapter.itemCount == 0)
                if (showEmpty) {
                    subcategoriesConcatAdapter.addAdapter(EmptyItemAdapter(R.string.subcategory_empty))
                }
            }
        }

        binding.categoryTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.showSubcategories = tab.position == 1
                if (viewModel.showSubcategories) {
                    binding.categoryRecycler.adapter = subcategoriesConcatAdapter
                } else {
                    binding.categoryRecycler.adapter = categoryMembersConcatAdapter
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.categoryTabLayout.selectTab(binding.categoryTabLayout.getTabAt(if (viewModel.showSubcategories) 1 else 0))
    }

    private fun loadPage(title: PageTitle) {
        if (viewModel.showSubcategories) {
            startActivity(newIntent(this, title))
        } else {
            val entry = HistoryEntry(title, HistoryEntry.SOURCE_CATEGORY)
            bottomSheetPresenter.show(supportFragmentManager, LinkPreviewDialog.newInstance(entry, null))
        }
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, entry.title) else PageActivity.newIntentForCurrentTab(this, entry, entry.title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, null, title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.showAddToListDialog(supportFragmentManager, title, InvokeSource.LINK_PREVIEW_MENU)
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
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

    private inner class CategoryMemberDiffCallback : DiffUtil.ItemCallback<PageTitle>() {
        override fun areItemsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return oldItem.prefixedText == newItem.prefixedText && oldItem.namespace == newItem.namespace
        }

        override fun areContentsTheSame(oldItem: PageTitle, newItem: PageTitle): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private inner class CategoryMembersAdapter : PagingDataAdapter<PageTitle, RecyclerView.ViewHolder>(CategoryMemberDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): CategoryItemHolder {
            val view = PageItemView<PageTitle>(this@CategoryActivity)
            view.callback = itemCallback
            return CategoryItemHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            getItem(position)?.let {
                (holder as CategoryItemHolder).bindItem(it)
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

    private inner class CategoryItemHolder constructor(val view: PageItemView<PageTitle>) : RecyclerView.ViewHolder(view) {
        fun bindItem(title: PageTitle) {
            view.item = title
            view.setTitle(if (title.namespace() !== Namespace.CATEGORY) title.displayText else StringUtil.removeUnderscores(title.text))
            view.setImageUrl(title.thumbUrl)
            view.setImageVisible(!title.thumbUrl.isNullOrEmpty())
            view.setDescription(title.description)
        }
    }

    private inner class ItemCallback : PageItemView.Callback<PageTitle?> {
        override fun onClick(item: PageTitle?) {
            item?.let { loadPage(it) }
        }

        override fun onLongClick(item: PageTitle?): Boolean {
            return false
        }

        override fun onActionClick(item: PageTitle?, view: View) {}

        override fun onListChipClick(readingList: ReadingList) {}
    }

    companion object {
        const val EXTRA_TITLE = "categoryTitle"

        fun newIntent(context: Context, categoryTitle: PageTitle): Intent {
            return Intent(context, CategoryActivity::class.java)
                    .putExtra(EXTRA_TITLE, categoryTitle)
        }
    }
}
