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
                    viewModel.deleteTabsState.collect {
                        // TODO
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
                if (viewModel.hasTabs) {
                    MaterialAlertDialogBuilder(this).run {
                        setMessage(R.string.close_all_tabs_confirm)
                        setPositiveButton(R.string.close_all_tabs_confirm_yes) { _, _ ->
                            viewModel.closeTabs()
//                            L.d("All tabs removed.")
//                            val appTabs = TabHelper.list.toMutableList()
//                            TabHelper.list.clear()
//                            binding.tabCountsView.updateTabCount(false)
//                            binding.tabRecyclerView.adapter?.notifyItemRangeRemoved(0, appTabs.size)
//                            setResult(RESULT_LOAD_FROM_BACKSTACK)
//                            showUndoAllSnackbar(appTabs)
//                            cancelled = false
                        }
                        setNegativeButton(R.string.close_all_tabs_confirm_no, null)
                        .show()
                    }
                }
                true
            }
            R.id.menu_save_all_tabs -> {
                if (viewModel.hasTabs) {
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
        binding.tabRecyclerView.adapter = TabItemAdapter(list)
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

    private fun showUndoSnackbar(index: Int, appTab: Tab, adapterPosition: Int) {
        appTab.getBackStackPositionTitle()?.let {
            FeedbackUtil.makeSnackbar(this, getString(R.string.tab_item_closed, it.displayText)).run {
                setAction(R.string.reading_list_item_delete_undo) {
                    TabHelper.list.add(index, appTab)
                    binding.tabRecyclerView.adapter?.notifyItemInserted(adapterPosition)
                    binding.tabCountsView.updateTabCount(false)
                    if (adapterPosition == 0 && TabHelper.hasTabs()) {
                        binding.tabRecyclerView.adapter?.notifyItemChanged(1)
                    }
                }
                show()
            }
        }
    }

    private fun showUndoAllSnackbar(appTabs: MutableList<Tab>) {
        FeedbackUtil.makeSnackbar(this, getString(R.string.all_tab_items_closed)).run {
            setAction(R.string.reading_list_item_delete_undo) {
                TabHelper.list.addAll(appTabs)
                appTabs.clear()
                binding.tabRecyclerView.adapter?.notifyItemRangeInserted(0, TabHelper.list.size)
                binding.tabCountsView.updateTabCount(false)
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

    private fun adapterPositionToTabIndex(list: List<Tab>, adapterPosition: Int): Int {
        return list.size - adapterPosition - 1
    }

    private open inner class TabViewHolder(itemView: View, val list: List<Tab>) : RecyclerView.ViewHolder(itemView), View.OnClickListener, SwipeableTabTouchHelperCallback.Callback {
        open fun bindItem(tab: Tab, position: Int) {
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
            val adapterPosition = bindingAdapterPosition
            val index = adapterPositionToTabIndex(list, adapterPosition)
            if (index < 0 || index >= TabHelper.list.size) {
                return
            }
            if (v.id == R.id.tabContainer) {
                if (index < TabHelper.list.size - 1) {
                    val tab = TabHelper.list.removeAt(index)
                    TabHelper.list.add(tab)
                }
                cancelled = false
                if (launchedFromPageActivity) {
                    setResult(RESULT_LOAD_FROM_BACKSTACK)
                } else {
                    startActivity(PageActivity.newIntent(this@TabActivity))
                }
                finish()
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
            val adapterPosition = bindingAdapterPosition
            val index = adapterPositionToTabIndex(list, adapterPosition)
            val appTab = TabHelper.list.removeAt(index)
            binding.tabCountsView.updateTabCount(false)
            bindingAdapter?.notifyItemRemoved(adapterPosition)
            if (adapterPosition == 0) {
                bindingAdapter?.notifyItemChanged(0)
            }
            setResult(RESULT_LOAD_FROM_BACKSTACK)
            showUndoSnackbar(index, appTab, adapterPosition)
        }
    }

    private inner class TabItemAdapter(val list: List<Tab>) : RecyclerView.Adapter<TabViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            val itemView = layoutInflater.inflate(R.layout.item_tab_contents, parent, false)
            return TabViewHolder(itemView, list)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            holder.bindItem(list[adapterPositionToTabIndex(list, position)], position)
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
