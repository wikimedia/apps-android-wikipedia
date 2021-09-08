package org.wikipedia.categories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityCategoryBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.PageItemView

class CategoryActivity : BaseActivity(), LinkPreviewDialog.Callback {
    private lateinit var binding: ActivityCategoryBinding
    private lateinit var categoryTitle: PageTitle
    private val unsortedTitleList = mutableListOf<PageTitle>()
    private val unsortedSubcategoryList = mutableListOf<PageTitle>()
    private val titleList = mutableListOf<PageTitle>()
    private val itemCallback = ItemCallback()
    private var showSubcategories = false
    private val pendingItemsForHydration = mutableListOf<PageTitle>()
    private val disposables = CompositeDisposable()
    private val hydrationRunnable = Runnable { hydrateTitles() }
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.categoryRecycler.layoutManager = LinearLayoutManager(this)
        binding.categoryRecycler.adapter = CategoryAdapter()
        categoryTitle = intent.getParcelableExtra(EXTRA_TITLE)!!
        supportActionBar?.title = categoryTitle.displayText

        binding.categoryTabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showSubcategories = tab.position == 1
                layOutTitles()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        loadCategory()
    }

    public override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // getMenuInflater().inflate(R.menu.menu_tabs, menu);
        return true
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
        if (showSubcategories /* title.namespace() === Namespace.CATEGORY */) {
            startActivity(newIntent(this, title))
        } else {
            val entry = HistoryEntry(title, HistoryEntry.SOURCE_CATEGORY)
            bottomSheetPresenter.show(supportFragmentManager, LinkPreviewDialog.newInstance(entry, null))
        }
    }

    private fun loadCategory() {
        disposables.clear()
        binding.categoryError.visibility = View.GONE
        binding.categoryRecycler.visibility = View.GONE
        binding.categoryProgress.visibility = View.VISIBLE
        disposables.add(ServiceFactory.get(categoryTitle.wikiSite).getCategoryMembers(categoryTitle.prefixedText, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate { binding.categoryProgress.visibility = View.GONE }
                .subscribe({ response ->
                    unsortedTitleList.clear()
                    for (page in response.query!!.categorymembers!!) {
                        val title = PageTitle(page.title, categoryTitle.wikiSite)
                        if (page.namespace() == Namespace.CATEGORY) {
                            unsortedSubcategoryList.add(title)
                        } else {
                            unsortedTitleList.add(title)
                        }
                    }
                    layOutTitles()
                }) { t ->
                    binding.categoryError.setError(t)
                    binding.categoryError.visibility = View.VISIBLE
                    L.e(t)
                })
    }

    private fun layOutTitles() {
        titleList.clear()
        titleList.addAll(if (showSubcategories) unsortedSubcategoryList else unsortedTitleList)
        if (titleList.isEmpty()) {
            binding.categoryRecycler.visibility = View.GONE
        }
        binding.categoryRecycler.visibility = View.VISIBLE
        binding.categoryError.visibility = View.GONE
        binding.categoryRecycler.adapter?.notifyDataSetChanged()
    }

    private fun queueForHydration(title: PageTitle) {
        val maxQueueSize = 50
        val runnableDelay = 500
        if (title.description != null || title.namespace() !== Namespace.MAIN) {
            return
        }
        pendingItemsForHydration.add(title)
        binding.categoryRecycler.removeCallbacks(hydrationRunnable)
        if (pendingItemsForHydration.size >= maxQueueSize) {
            hydrateTitles()
        } else {
            binding.categoryRecycler.postDelayed(hydrationRunnable, runnableDelay.toLong())
        }
    }

    private fun hydrateTitles() {
        val titles: List<PageTitle?> = ArrayList(pendingItemsForHydration)
        pendingItemsForHydration.clear()
        disposables.add(ServiceFactory.get(categoryTitle.wikiSite).getImagesAndThumbnails(titles.joinToString("|"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    for (page in response.query!!.pages!!) {
                        for (title in titles) {
                            if (title!!.displayText == page.title) {
                                title.thumbUrl = page.thumbUrl()
                                title.description = if (page.description.isNullOrEmpty()) "" else page.description
                                break
                            }
                        }
                    }
                    binding.categoryRecycler.adapter?.notifyDataSetChanged()
                }) { L.e(it) })
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

    private inner class CategoryItemHolder constructor(itemView: PageItemView<*>) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(title: PageTitle) {
            view.item = title
            view.setTitle(if (title.namespace() !== Namespace.CATEGORY) title.displayText else title.text.replace("_", " "))
            view.setImageUrl(title.thumbUrl)
            view.setDescription(title.description)
        }

        val view: PageItemView<PageTitle>
            get() = itemView as PageItemView<PageTitle>
    }

    private inner class CategoryAdapter : RecyclerView.Adapter<CategoryItemHolder>() {
        override fun getItemCount(): Int {
            return titleList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): CategoryItemHolder {
            val view = PageItemView<PageTitle>(this@CategoryActivity)
            return CategoryItemHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryItemHolder, pos: Int) {
            holder.bindItem(titleList[pos])
            queueForHydration(titleList[pos])
        }

        override fun onViewAttachedToWindow(holder: CategoryItemHolder) {
            super.onViewAttachedToWindow(holder)
            holder.view.callback = itemCallback
        }

        override fun onViewDetachedFromWindow(holder: CategoryItemHolder) {
            holder.view.callback = null
            super.onViewDetachedFromWindow(holder)
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
        private const val EXTRA_TITLE = "categoryTitle"

        @JvmStatic
        fun newIntent(context: Context, categoryTitle: PageTitle): Intent {
            return Intent(context, CategoryActivity::class.java)
                    .putExtra(EXTRA_TITLE, categoryTitle)
        }
    }
}
