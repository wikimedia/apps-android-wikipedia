package org.wikipedia.main

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.ActivityMainBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedFragment
import org.wikipedia.feed.HomeFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.page.PageActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class MainActivity : SingleFragmentActivity<MainFragment>(), MainFragment.Callback {

    private lateinit var binding: ActivityMainBinding

    private var statusBarInsets: Insets? = null
    private var controlNavTabInFragment = false
    private val onboardingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val fragment = fragment.currentFragment
        if (it.resultCode == InitialOnboardingActivity.RESULT_LANGUAGE_CHANGED && fragment is FeedFragment) {
            fragment.refresh()
        }
    }

    override fun inflateAndSetContentView() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeviceUtil.assertAppContext(this)) {
            return
        }

        disableFitsSystemWindows()
        enableEdgeToEdge()
        DeviceUtil.setLightSystemUiVisibility(this)

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            statusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.root.updatePadding(bottom = navBarInsets.bottom)

            applyStatusBarInsets()
            WindowInsetsCompat.CONSUMED.toWindowInsets()!!
        }

        onBackPressedDispatcher.addCallback(this) {
            if (fragment.onBackPressed()) {
                return@addCallback
            }
            finish()
        }

        setImageZoomHelper()
        if (Prefs.isInitialOnboardingEnabled && savedInstanceState == null &&
            !intent.hasExtra(Constants.INTENT_EXTRA_PREVIEW_SAVED_READING_LISTS)) {
            onboardingLauncher.launch(InitialOnboardingActivity.newIntent(this))
        }
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setSupportActionBar(binding.mainToolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.mainToolbar.navigationIcon = null

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun createFragment(): MainFragment {
        return MainFragment.newInstance()
    }

    override fun onTabChanged(tab: NavTab) {
        if (tab == NavTab.EXPLORE) {
            binding.mainToolbar.isVisible = false
            binding.mainToolbarWordmark.visibility = View.VISIBLE
            binding.mainToolbar.title = ""
            controlNavTabInFragment = false

            applyNavBarTheme(Theme.DARK)
        } else {
            if (tab == NavTab.SEARCH && Prefs.showSearchTabTooltip) {
                FeedbackUtil.showTooltip(this, fragment.binding.mainNavTabLayout.findViewById(NavTab.SEARCH.id), getString(R.string.search_tab_tooltip), aboveOrBelow = true, autoDismiss = false)
                Prefs.showSearchTabTooltip = false
            }
            if (tab == NavTab.EDITS) {
                ImageRecommendationsEvent.logImpression("suggested_edit_dialog")
                PatrollerExperienceEvent.logImpression("suggested_edits_dialog")
            }
            binding.mainToolbarWordmark.visibility = View.GONE
            binding.mainToolbar.setTitle(tab.text)
            binding.mainToolbar.isVisible = true
            controlNavTabInFragment = true

            applyNavBarTheme(WikipediaApp.instance.currentTheme)
        }
        applyStatusBarInsets()
        fragment.requestUpdateToolbarElevation()
    }

    private fun applyStatusBarInsets() {
        statusBarInsets?.let {
            binding.root.updatePadding(top = if (fragment.currentFragment is HomeFragment) 0 else it.top)
        }
    }

    private fun applyNavBarTheme(theme: Theme) {
        val wrapper = ContextThemeWrapper(this, theme.resourceId)
        val attrs = intArrayOf(R.attr.paper_color, R.attr.border_color)
        val csl = AppCompatResources.getColorStateList(wrapper, R.color.color_state_nav_tab)
        wrapper.withStyledAttributes(null, attrs) {
            val paperColor = getColor(0, Color.TRANSPARENT)
            val borderColor = getColor(1, Color.TRANSPARENT)
            fragment.binding.mainNavTabLayout.applyColors(paperColor, csl)
            fragment.binding.mainNavTabBorder.setBackgroundColor(borderColor)

            binding.root.setBackgroundColor(paperColor)
            DeviceUtil.setLightSystemUiVisibility(this@MainActivity, light = !theme.isDark)
        }
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        if (!controlNavTabInFragment) {
            fragment.setBottomNavVisible(false)
        }
    }

    override fun onSupportActionModeFinished(mode: ActionMode) {
        super.onSupportActionModeFinished(mode)
        fragment.setBottomNavVisible(true)
    }

    override fun updateToolbarElevation(elevate: Boolean) {
        if (elevate) {
            setToolbarElevationDefault()
        } else {
            clearToolbarElevation()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        fragment.handleIntent(intent)
    }

    override fun onGoOffline() {
        fragment.onGoOffline()
    }

    override fun onGoOnline() {
        fragment.onGoOnline()
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            // TODO: handle special cases of non-article content, e.g. shared reading lists.
            intent.data?.let {
                if (it.authority.orEmpty().endsWith(WikiSite.BASE_DOMAIN)) {
                    // Pass it right along to PageActivity
                    val uri = Uri.parse(it.toString().replace("wikipedia://", WikiSite.DEFAULT_SCHEME + "://"))
                    startActivity(Intent(this, PageActivity::class.java)
                            .setAction(Intent.ACTION_VIEW)
                            .setData(uri))
                }
            }
        }
    }

    fun isCurrentFragmentSelected(f: Fragment): Boolean {
        return fragment.currentFragment === f
    }

    fun getToolbar(): Toolbar {
        return binding.mainToolbar
    }

    fun getStatusBarInsets(): Insets? {
        return statusBarInsets
    }

    override fun onUnreadNotification() {
        fragment.updateNotificationDot(true)
    }

    private fun setToolbarElevationDefault() {
        binding.mainToolbar.elevation = DimenUtil.dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation))
    }

    private fun clearToolbarElevation() {
        binding.mainToolbar.elevation = 0f
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}
