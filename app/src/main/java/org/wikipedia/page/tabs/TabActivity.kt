package org.wikipedia.page.tabs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTabsBinding
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.WikiCardView

class TabActivity : BaseActivity() {
    private lateinit var binding: ActivityTabsBinding

    private val viewModel: TabViewModel by viewModels()
    private var launchedFromPageActivity = false
    private var cancelled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tabRecyclerView.adapter = TabItemAdapter()
        binding.tabCountsView.updateTabCount(false)
        binding.tabCountsView.setOnClickListener { onBackPressed() }
        FeedbackUtil.setButtonTooltip(binding.tabCountsView, binding.tabButtonNotifications)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> onLoading()
                            is Resource.Success -> onSuccess(it.data)
                            is Resource.Error -> onError(it.throwable)
                        }
                    }
                }

                launch {
                    viewModel.undoTabsState.collect {
                        when (it) {
                            is Resource.Success -> {
                                (binding.tabRecyclerView.adapter as TabItemAdapter).setList(it.data.second)
                                binding.tabRecyclerView.adapter?.notifyItemInserted(it.data.first)
                                binding.tabRecyclerView.adapter?.notifyItemRangeChanged(0, viewModel.list.size)
                                binding.tabCountsView.updateTabCount(false)
                            }

                            is Resource.Error -> {
                                FeedbackUtil.showError(this@TabActivity, it.throwable)
                            }
                        }
                    }
                }

                launch {
                    viewModel.deleteTabsState.collect {
                        when (it) {
                            is Resource.Success -> {
                                binding.tabCountsView.updateTabCount(false)
                                (binding.tabRecyclerView.adapter as TabItemAdapter).setList(viewModel.list)
                                val firstIndex = it.data.first.indexOfFirst { tab -> tab.id == it.data.second.firstOrNull()?.id }
                                binding.tabRecyclerView.adapter?.notifyItemRangeRemoved(firstIndex, it.data.second.size)
                                binding.tabRecyclerView.adapter?.notifyItemRangeChanged(0, viewModel.list.size)
                                setResult(RESULT_LOAD_FROM_BACKSTACK)
                                showUndoSnackbar(firstIndex, it.data.first, it.data.second)
                                cancelled = false
                            }

                            is Resource.Error -> {
                                FeedbackUtil.showError(this@TabActivity, it.throwable)
                            }
                        }
                    }
                }

                launch {
                    viewModel.saveToListState.collect {
                        when (it) {
                            is Resource.Success -> {
                                ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                                    AddToReadingListDialog.newInstance(it.data, InvokeSource.TABS_ACTIVITY))
                            }

                            is Resource.Error -> {
                                FeedbackUtil.showError(this@TabActivity, it.throwable)
                            }
                        }
                    }
                }

                launch {
                    viewModel.clickState.collect {
                        when (it) {
                            is Resource.Success -> {
                                cancelled = false
                                if (launchedFromPageActivity) {
                                    setResult(RESULT_LOAD_FROM_BACKSTACK)
                                } else {
                                    startActivity(PageActivity.newIntent(this@TabActivity))
                                }
                                finish()
                            }

                            is Resource.Error -> {
                                FeedbackUtil.showError(this@TabActivity, it.throwable)
                            }
                        }
                    }
                }
            }
        }

        val touchCallback = SwipeableTabTouchHelperCallback(this)
        touchCallback.swipeableEnabled = true
        val itemTouchHelper = ItemTouchHelper(touchCallback)
        itemTouchHelper.attachToRecyclerView(binding.tabRecyclerView)

        launchedFromPageActivity = intent.hasExtra(LAUNCHED_FROM_PAGE_ACTIVITY)
        setStatusBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))
        setSupportActionBar(binding.tabToolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        binding.tabButtonNotifications.setOnClickListener {
            if (AccountUtil.isLoggedIn) {
                startActivity(NotificationActivity.newIntent(this))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationsButton(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tabs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home, R.id.menu_open_a_new_tab -> {
                openNewTab()
                true
            }
            R.id.menu_close_all_tabs -> {
                if (viewModel.list.isNotEmpty()) {
                    MaterialAlertDialogBuilder(this).run {
                        setMessage(R.string.close_all_tabs_confirm)
                        setPositiveButton(R.string.close_all_tabs_confirm_yes) { _, _ ->
                            viewModel.deleteTabs(viewModel.list.toList())
                        }
                        setNegativeButton(R.string.close_all_tabs_confirm_no, null)
                        .show()
                    }
                }
                true
            }
            R.id.menu_save_all_tabs -> {
                if (viewModel.list.isNotEmpty()) {
                    viewModel.saveToList()
                }
                true
            }
            R.id.menu_explore -> {
                goToMainTab()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onUnreadNotification() {
        updateNotificationsButton(true)
    }

    private fun onLoading() {
        binding.tabRecyclerView.isVisible = false
        binding.errorView.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun onSuccess(list: List<Tab>) {
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false
        binding.tabRecyclerView.isVisible = true
        (binding.tabRecyclerView.adapter as TabItemAdapter).setList(list)
        binding.tabRecyclerView.adapter?.notifyItemRangeChanged(0, list.size)
        binding.tabCountsView.updateTabCount(false)
    }

    private fun onError(throwable: Throwable) {
        L.e(throwable)
        binding.progressBar.isVisible = false
        binding.tabRecyclerView.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.backClickListener = View.OnClickListener {
            finish()
        }
    }

    private fun openNewTab() {
        cancelled = false
        if (launchedFromPageActivity) {
            setResult(RESULT_NEW_TAB)
        } else {
            startActivity(PageActivity.newIntentForNewTab(this@TabActivity))
        }
        finish()
    }

    private fun showUndoSnackbar(undoPosition: Int, originalTabs: List<Tab>, deletedTabs: List<Tab>) {
        val snackBarMessage = if (deletedTabs.size == 1) {
            getString(R.string.tab_item_closed, deletedTabs.first().getBackStackPositionTitle()?.displayText.orEmpty())
        } else {
            getString(R.string.all_tab_items_closed)
        }
        FeedbackUtil.makeSnackbar(this, snackBarMessage).run {
            setAction(R.string.reading_list_item_delete_undo) {
                viewModel.undoDeleteTabs(undoPosition, originalTabs)
            }
            show()
        }
    }

    private fun goToMainTab() {
        startActivity(
            MainActivity.newIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
                .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()))
        finish()
    }

    private fun updateNotificationsButton(animate: Boolean) {
        if (AccountUtil.isLoggedIn) {
            binding.tabButtonNotifications.isVisible = true
            if (Prefs.notificationUnreadCount > 0) {
                binding.tabButtonNotifications.setUnreadCount(Prefs.notificationUnreadCount)
                if (animate) {
                    binding.tabButtonNotifications.runAnimation()
                }
            } else {
                binding.tabButtonNotifications.setUnreadCount(0)
            }
        } else {
            binding.tabButtonNotifications.isVisible = false
        }
    }

    private open inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, SwipeableTabTouchHelperCallback.Callback {
        lateinit var tab: Tab
        open fun bindItem(tab: Tab, position: Int) {
            this.tab = tab
            itemView.findViewById<TextView>(R.id.tabArticleTitle).text = StringUtil.fromHtml(tab.getBackStackPositionTitle()?.displayText.orEmpty())
            itemView.findViewById<TextView>(R.id.tabArticleDescription).text = StringUtil.fromHtml(tab.getBackStackPositionTitle()?.description.orEmpty())
            itemView.findViewById<View>(R.id.tabContainer).setOnClickListener(this)
            itemView.findViewById<View>(R.id.tabCloseButton).setOnClickListener(this)
            itemView.findViewById<WikiCardView>(R.id.tabCardView).run {
                strokeWidth = DimenUtil.roundedDpToPx(if (position == 0) 1.5f else 1f)
                strokeColor = ResourceUtil.getThemedColor(context, if (position == 0) R.attr.progressive_color else R.attr.border_color)
            }
        }

        override fun onClick(v: View) {
            if (v.id == R.id.tabContainer) {
                viewModel.moveTabToForeground(tab)
            } else if (v.id == R.id.tabCloseButton) {
                doCloseTab()
            }
        }

        override fun onSwipe() {
            doCloseTab()
        }

        override fun isSwipeable(): Boolean {
            return true
        }

        private fun doCloseTab() {
            viewModel.deleteTabs(listOf(tab))
        }
    }

    private inner class TabItemAdapter() : RecyclerView.Adapter<TabViewHolder>() {
        private var list: List<Tab> = emptyList()

        fun setList(newList: List<Tab>) {
            list = newList
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            val itemView = layoutInflater.inflate(R.layout.item_tab_contents, parent, false)
            return TabViewHolder(itemView)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            holder.bindItem(list[position], position)
        }
    }

    companion object {
        private const val LAUNCHED_FROM_PAGE_ACTIVITY = "launchedFromPageActivity"
        const val RESULT_LOAD_FROM_BACKSTACK = 10
        const val RESULT_NEW_TAB = 11

        fun newIntent(context: Context): Intent {
            return Intent(context, TabActivity::class.java)
        }

        fun newIntentFromPageActivity(context: Context): Intent {
            return Intent(context, TabActivity::class.java)
                    .putExtra(LAUNCHED_FROM_PAGE_ACTIVITY, true)
        }
    }
}
