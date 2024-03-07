package org.wikipedia.page.tabs

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTabsBinding
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.log.L

class TabActivity : BaseActivity() {
    private lateinit var binding: ActivityTabsBinding
    private val app: WikipediaApp = WikipediaApp.instance
    private var launchedFromPageActivity = false
    private var cancelled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tabCountsView.updateTabCount(false)
        binding.tabCountsView.setOnClickListener { onBackPressed() }
        FeedbackUtil.setButtonLongPressToast(binding.tabCountsView, binding.tabButtonNotifications)


        binding.tabRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.tabRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.tabRecyclerView.adapter = TabItemAdapter()

        /*
        binding.tabSwitcher.decorator = object : TabSwitcherDecorator() {
            override fun onInflateView(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): View {
                if (viewType == 1) {
                    val view = AppCompatImageView(this@TabActivity)
                    view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    view.scaleType = ImageView.ScaleType.CENTER_CROP
                    view.setImageBitmap(FIRST_TAB_BITMAP)
                    view.setPadding(0, if (topTabLeadImageEnabled()) 0 else -DimenUtil.getToolbarHeightPx(this@TabActivity), 0, 0)
                    return view
                }
                return inflater.inflate(R.layout.item_tab_contents, parent, false)
            }

            override fun onShowTab(context: Context, tabSwitcher: TabSwitcher, view: View,
                                   tab: de.mrapp.android.tabswitcher.Tab, index: Int, viewType: Int, savedInstanceState: Bundle?) {
                val tabIndex = app.tabCount - index - 1
                if (viewType == 1 || tabIndex < 0 || app.tabList[tabIndex] == null) {
                    return
                }
                val titleText = view.findViewById<TextView>(R.id.tab_article_title)
                val descriptionText = view.findViewById<TextView>(R.id.tab_article_description)
                val title = app.tabList[tabIndex].backStackPositionTitle
                titleText.text = StringUtil.fromHtml(title!!.displayText)
                if (title.description.isNullOrEmpty()) {
                    descriptionText.visibility = View.GONE
                } else {
                    descriptionText.text = title.description
                    descriptionText.visibility = View.VISIBLE
                }
                L10nUtil.setConditionalLayoutDirection(view, title.wikiSite.languageCode)
            }

            override fun getViewType(tab: de.mrapp.android.tabswitcher.Tab, index: Int): Int {
                return if (FIRST_TAB_BITMAP_TITLE == app.tabList[app.tabCount - index - 1]?.backStackPositionTitle?.prefixedText) {
                    1
                } else {
                    0
                }
            }

            override fun getViewTypeCount(): Int {
                return 2
            }
        }
        */


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

    override fun onDestroy() {
        clearFirstTabBitmap()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        app.commitTabState()
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
                if (app.tabList.isNotEmpty()) {
                    MaterialAlertDialogBuilder(this).run {
                        setMessage(R.string.close_all_tabs_confirm)
                        setPositiveButton(R.string.close_all_tabs_confirm_yes) { _, _ ->
                            L.d("All tabs removed.")
                            val appTabs = app.tabList.toMutableList()
                            app.tabList.clear()
                            binding.tabCountsView.updateTabCount(false)
                            setResult(RESULT_LOAD_FROM_BACKSTACK)
                            showUndoAllSnackbar(appTabs)
                            cancelled = false
                        }
                        setNegativeButton(R.string.close_all_tabs_confirm_no, null)
                        .show()
                    }
                }
                true
            }
            R.id.menu_save_all_tabs -> {
                if (app.tabList.isNotEmpty()) {
                    saveTabsToList()
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

    private fun saveTabsToList() {
        val titlesList = app.tabList.filter { it.backStackPositionTitle != null }.map { it.backStackPositionTitle!! }
        ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(titlesList, InvokeSource.TABS_ACTIVITY))
    }

    private fun topTabLeadImageEnabled(): Boolean {
        if (app.tabCount > 0) {
            val pageTitle = app.tabList[app.tabCount - 1].backStackPositionTitle
            return pageTitle != null && !pageTitle.isMainPage && !pageTitle.thumbUrl.isNullOrEmpty()
        }
        return false
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

    private fun showUndoSnackbar(index: Int, appTab: Tab, appTabIndex: Int) {
        appTab.backStackPositionTitle?.let {
            FeedbackUtil.makeSnackbar(this, getString(R.string.tab_item_closed, it.displayText)).run {
                setAction(R.string.reading_list_item_delete_undo) {
                    app.tabList.add(appTabIndex, appTab)
                    // TODO
                }
                show()
            }
        }
    }

    private fun showUndoAllSnackbar(appTabs: MutableList<Tab>) {
        FeedbackUtil.makeSnackbar(this, getString(R.string.all_tab_items_closed)).run {
            setAction(R.string.reading_list_item_delete_undo) {
                app.tabList.addAll(appTabs)
                appTabs.clear()
            }
            show()
        }
    }

    /*
    private inner class TabListener : TabSwitcherListener {
        override fun onSwitcherShown(tabSwitcher: TabSwitcher) {}
        override fun onSwitcherHidden(tabSwitcher: TabSwitcher) {}
        override fun onSelectionChanged(tabSwitcher: TabSwitcher, index: Int, selectedTab: de.mrapp.android.tabswitcher.Tab?) {
            if (app.tabList.isNotEmpty() && index < app.tabList.size) {
                val tabIndex = app.tabList.size - index - 1
                L.d("Tab selected: $index")
                if (tabIndex < app.tabList.size - 1) {
                    val tab = app.tabList.removeAt(tabIndex)
                    app.tabList.add(tab)
                }
                binding.tabCountsView.updateTabCount(false)
                cancelled = false

                    if (launchedFromPageActivity) {
                        setResult(RESULT_LOAD_FROM_BACKSTACK)
                    } else {
                        startActivity(PageActivity.newIntent(this@TabActivity))
                    }
                    finish()
            }
        }

        override fun onTabAdded(tabSwitcher: TabSwitcher, index: Int, tab: de.mrapp.android.tabswitcher.Tab, animation: Animation) {
            binding.tabCountsView.updateTabCount(false)
        }

        override fun onTabRemoved(tabSwitcher: TabSwitcher, index: Int, tab: de.mrapp.android.tabswitcher.Tab, animation: Animation) {
            if (app.tabList.isNotEmpty() && index < app.tabList.size) {
                val tabIndex = app.tabList.size - index - 1
                val appTab = app.tabList.removeAt(tabIndex)
                binding.tabCountsView.updateTabCount(false)
                setResult(RESULT_LOAD_FROM_BACKSTACK)
                showUndoSnackbar(tab, index, appTab, tabIndex)
            }
        }

        override fun onAllTabsRemoved(tabSwitcher: TabSwitcher, tabs: Array<de.mrapp.android.tabswitcher.Tab>, animation: Animation) {

        }
    }
    */

    private fun goToMainTab() {
        startActivity(MainActivity.newIntent(this)
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

    private open inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindItem(tab: Tab) {
            itemView.findViewById<TextView>(R.id.section_header_text).text = StringUtil.fromHtml(tab.backStackPositionTitle?.displayText.orEmpty())
        }
    }

    private inner class TabItemAdapter : RecyclerView.Adapter<TabViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            return TabViewHolder(layoutInflater.inflate(R.layout.view_section_header, parent, false))
        }

        override fun getItemCount(): Int {
            return app.tabList.size
        }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            holder.bindItem(app.tabList[position])
        }
    }

    companion object {
        private const val LAUNCHED_FROM_PAGE_ACTIVITY = "launchedFromPageActivity"
        private var FIRST_TAB_BITMAP: Bitmap? = null
        private var FIRST_TAB_BITMAP_TITLE = ""
        const val RESULT_LOAD_FROM_BACKSTACK = 10
        const val RESULT_NEW_TAB = 11

        fun captureFirstTabBitmap(view: View, title: String) {
            clearFirstTabBitmap()
            try {
                if (view.isLaidOut) {
                    FIRST_TAB_BITMAP = view.drawToBitmap(Bitmap.Config.RGB_565)
                    FIRST_TAB_BITMAP_TITLE = title
                }
            } catch (e: OutOfMemoryError) {
                // don't worry about it
            }
        }

        private fun clearFirstTabBitmap() {
            FIRST_TAB_BITMAP_TITLE = ""
            FIRST_TAB_BITMAP?.run {
                if (!isRecycled) {
                    recycle()
                }
                FIRST_TAB_BITMAP = null
            }
        }

        fun newIntent(context: Context): Intent {
            return Intent(context, TabActivity::class.java)
        }

        fun newIntentFromPageActivity(context: Context): Intent {
            return Intent(context, TabActivity::class.java)
                    .putExtra(LAUNCHED_FROM_PAGE_ACTIVITY, true)
        }
    }
}
