package org.wikipedia.page.tabs

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.scale
import androidx.core.view.isVisible
import de.mrapp.android.tabswitcher.Animation
import de.mrapp.android.tabswitcher.TabSwitcher
import de.mrapp.android.tabswitcher.TabSwitcherDecorator
import de.mrapp.android.tabswitcher.TabSwitcherListener
import de.mrapp.android.util.logging.LogLevel
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.TabFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ActivityTabsBinding
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.log.L

class TabActivity : BaseActivity() {
    private lateinit var binding: ActivityTabsBinding
    private val app: WikipediaApp = WikipediaApp.getInstance()
    private val tabListener = TabListener()
    private val funnel = TabFunnel()
    private var launchedFromPageActivity = false
    private var cancelled = true
    private var tabUpdatedTimeMillis: Long = 0
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        funnel.logEnterList(app.tabCount)
        binding.tabCountsView.updateTabCount(false)
        binding.tabCountsView.setOnClickListener { onBackPressed() }
        FeedbackUtil.setButtonLongPressToast(binding.tabCountsView, binding.tabButtonNotifications)
        binding.tabSwitcher.setPreserveState(false)
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
                return if (index == 0 && FIRST_TAB_BITMAP != null && !FIRST_TAB_BITMAP!!.isRecycled) {
                    1
                } else {
                    0
                }
            }

            override fun getViewTypeCount(): Int {
                return 2
            }
        }
        for (i in app.tabList.indices) {
            val tabIndex = app.tabList.size - i - 1
            if (app.tabList[tabIndex].backStack.isEmpty()) {
                continue
            }
            val tab = de.mrapp.android.tabswitcher.Tab(StringUtil.fromHtml(app.tabList[tabIndex].backStackPositionTitle?.displayText))
            tab.setIcon(R.drawable.ic_image_black_24dp)
            tab.setIconTint(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color))
            tab.setTitleTextColor(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color))
            tab.setCloseButtonIcon(R.drawable.ic_close_white_24dp)
            tab.setCloseButtonIconTint(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color))
            tab.isCloseable = true
            tab.parameters = Bundle()
            binding.tabSwitcher.addTab(tab)
        }
        binding.tabSwitcher.logLevel = LogLevel.OFF
        binding.tabSwitcher.addListener(tabListener)
        binding.tabSwitcher.showSwitcher()
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
        if (cancelled) {
            funnel.logCancel(app.tabCount)
        }
        binding.tabSwitcher.removeListener(tabListener)
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
                    AlertDialog.Builder(this).run {
                        setMessage(R.string.close_all_tabs_confirm)
                        setPositiveButton(R.string.close_all_tabs_confirm_yes) { _, _ ->
                            binding.tabSwitcher.clear()
                            cancelled = false
                        }
                        setNegativeButton(R.string.close_all_tabs_confirm_no, null)
                        create().show()
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
        bottomSheetPresenter.show(supportFragmentManager,
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
        funnel.logCreateNew(app.tabCount)
        if (launchedFromPageActivity) {
            setResult(RESULT_NEW_TAB)
        } else {
            startActivity(PageActivity.newIntentForNewTab(this@TabActivity))
        }
        finish()
    }

    private fun showUndoSnackbar(tab: de.mrapp.android.tabswitcher.Tab, index: Int, appTab: Tab, appTabIndex: Int) {
        appTab.backStackPositionTitle?.let {
            FeedbackUtil.makeSnackbar(this, getString(R.string.tab_item_closed, it.displayText), FeedbackUtil.LENGTH_DEFAULT).run {
                setAction(R.string.reading_list_item_delete_undo) {
                    app.tabList.add(appTabIndex, appTab)
                    binding.tabSwitcher.addTab(tab, index)
                }
                show()
            }
        }
    }

    private fun showUndoAllSnackbar(tabs: Array<de.mrapp.android.tabswitcher.Tab>, appTabs: MutableList<Tab>) {
        FeedbackUtil.makeSnackbar(this, getString(R.string.all_tab_items_closed), FeedbackUtil.LENGTH_DEFAULT).run {
            setAction(R.string.reading_list_item_delete_undo) {
                app.tabList.addAll(appTabs)
                binding.tabSwitcher.addAllTabs(tabs)
                appTabs.clear()
            }
            show()
        }
    }

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
                val tabUpdateDebounceMillis = 250
                if (System.currentTimeMillis() - tabUpdatedTimeMillis > tabUpdateDebounceMillis) {
                    funnel.logSelect(app.tabCount, tabIndex)
                    if (launchedFromPageActivity) {
                        setResult(RESULT_LOAD_FROM_BACKSTACK)
                    } else {
                        startActivity(PageActivity.newIntent(this@TabActivity))
                    }
                    finish()
                }
            }
        }

        override fun onTabAdded(tabSwitcher: TabSwitcher, index: Int, tab: de.mrapp.android.tabswitcher.Tab, animation: Animation) {
            binding.tabCountsView.updateTabCount(false)
            tabUpdatedTimeMillis = System.currentTimeMillis()
        }

        override fun onTabRemoved(tabSwitcher: TabSwitcher, index: Int, tab: de.mrapp.android.tabswitcher.Tab, animation: Animation) {
            if (app.tabList.isNotEmpty() && index < app.tabList.size) {
                val tabIndex = app.tabList.size - index - 1
                val appTab = app.tabList.removeAt(tabIndex)
                funnel.logClose(app.tabCount, tabIndex)
                binding.tabCountsView.updateTabCount(false)
                setResult(RESULT_LOAD_FROM_BACKSTACK)
                showUndoSnackbar(tab, index, appTab, tabIndex)
                tabUpdatedTimeMillis = System.currentTimeMillis()
            }
        }

        override fun onAllTabsRemoved(tabSwitcher: TabSwitcher, tabs: Array<de.mrapp.android.tabswitcher.Tab>, animation: Animation) {
            L.d("All tabs removed.")
            val appTabs = app.tabList.toMutableList()
            app.tabList.clear()
            binding.tabCountsView.updateTabCount(false)
            setResult(RESULT_LOAD_FROM_BACKSTACK)
            showUndoAllSnackbar(tabs, appTabs)
            tabUpdatedTimeMillis = System.currentTimeMillis()
        }
    }

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

    companion object {
        private const val LAUNCHED_FROM_PAGE_ACTIVITY = "launchedFromPageActivity"
        private const val MAX_CACHED_BMP_SIZE = 800
        private var FIRST_TAB_BITMAP: Bitmap? = null
        const val RESULT_LOAD_FROM_BACKSTACK = 10
        const val RESULT_NEW_TAB = 11

        fun captureFirstTabBitmap(view: View) {
            clearFirstTabBitmap()
            var bmp: Bitmap? = null
            try {
                val wasCacheEnabled = view.isDrawingCacheEnabled
                if (!wasCacheEnabled) {
                    view.isDrawingCacheEnabled = true
                    view.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_LOW
                }
                val cacheBmp = view.drawingCache
                if (cacheBmp != null) {
                    val width: Int
                    val height: Int
                    if (cacheBmp.width > cacheBmp.height) {
                        width = MAX_CACHED_BMP_SIZE
                        height = width * cacheBmp.height / cacheBmp.width
                    } else {
                        height = MAX_CACHED_BMP_SIZE
                        width = height * cacheBmp.width / cacheBmp.height
                    }
                    bmp = cacheBmp.scale(width, height)
                }
                if (!wasCacheEnabled) {
                    view.isDrawingCacheEnabled = false
                }
            } catch (e: OutOfMemoryError) {
                // don't worry about it
            }
            FIRST_TAB_BITMAP = bmp
        }

        private fun clearFirstTabBitmap() {
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
