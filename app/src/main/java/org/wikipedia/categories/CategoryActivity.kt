package org.wikipedia.categories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class CategoryActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private lateinit var binding: ActivityCategoryBinding

    private val categoryMembersAdapter = CategoryMembersAdapter()
    private val itemCallback = ItemCallback()
    private var showSubcategories = false

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
        binding.categoryRecycler.addItemDecoration(DrawableItemDecoration(this, R.attr.list_separator_drawable, drawStart = true, drawEnd = false))
        binding.categoryRecycler.adapter = categoryMembersAdapter

        lifecycleScope.launch {
            viewModel.categoryMembersFlow.collectLatest {
                categoryMembersAdapter.submitData(it)
            }
        }

        /*
        binding.categoryTabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showSubcategories = tab.position == 1
                layOutTitles()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        */
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadPage(title: PageTitle) {
        if (showSubcategories) {
            startActivity(newIntent(this, title))
        } else {
            val entry = HistoryEntry(title, HistoryEntry.SOURCE_CATEGORY)
            bottomSheetPresenter.show(supportFragmentManager, LinkPreviewDialog.newInstance(entry, null))
        }
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, entry.title) else PageActivity.newIntentForCurrentTab(this, entry, entry.title))
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

    private inner class CategoryItemHolder constructor(itemView: PageItemView<*>) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(title: PageTitle) {
            view.item = title
            view.setTitle(if (title.namespace() !== Namespace.CATEGORY) title.displayText else StringUtil.removeUnderscores(title.text))
            view.setImageUrl(title.thumbUrl)
            view.setImageVisible(!title.thumbUrl.isNullOrEmpty())
            view.setDescription(title.description)
        }

        val view: PageItemView<PageTitle>
            get() = itemView as PageItemView<PageTitle>
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
