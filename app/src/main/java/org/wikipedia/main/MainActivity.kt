package org.wikipedia.main

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.addCallback
import io.bitdrift.capture.Capture.Logger
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.net.toUri
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
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.databinding.ActivityMainBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.HomeFragment
import org.wikipedia.feed.HomeTab
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.navtab.NavTab
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.widgets.readingchallenge.ReadingChallengeInstallWidgetDialog
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository

class MainActivity : SingleFragmentActivity<MainFragment>(), MainFragment.Callback {

    private lateinit var binding: ActivityMainBinding

    private var statusBarInsets = Insets.NONE
    private var navBarInsets = Insets.NONE
    private var controlNavTabInFragment = false
    private val onboardingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val fragment = fragment.currentFragment
        if (fragment is HomeFragment) {
            val tab = if (Prefs.homePreferenceSelection == HomePreferenceType.PERSONALIZED) HomeTab.FOR_YOU else HomeTab.COMMUNITY
            fragment.updateLanguage(Prefs.homeLanguageCode)
            fragment.selectTab(tab)
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
            navBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            applyInsets()
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
        setNavigationBarColor(Color.TRANSPARENT)
        setSupportActionBar(binding.mainToolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.mainToolbar.navigationIcon = null

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        binding.root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.root.viewTreeObserver.removeOnPreDrawListener(this)
                WikipediaApp.appStartTime?.let { Logger.logAppLaunchTTI(it.elapsedNow()) }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun createFragment(): MainFragment {
        return MainFragment.newInstance()
    }

    override fun onTabChanged(tab: NavTab) {
        if (tab == NavTab.HOME) {
            binding.mainToolbar.isVisible = false
            binding.mainToolbarWordmark.visibility = View.VISIBLE
            binding.mainToolbar.title = ""
            controlNavTabInFragment = false

            applyNavBarTheme(if ((fragment.currentFragment as? HomeFragment)?.getCurrentTab() == HomeTab.FOR_YOU) Theme.BLACK else WikipediaApp.instance.currentTheme)
        } else {
            if (tab == NavTab.SEARCH && Prefs.showSearchTabTooltip) {
                FeedbackUtil.showTooltip(this, fragment.binding.mainNavTabLayout.findViewById(NavTab.SEARCH.id), getString(R.string.search_tab_tooltip), aboveOrBelow = true, autoDismiss = false)
                Prefs.showSearchTabTooltip = false
            }
            if (tab == NavTab.EDITS) {
                ImageRecommendationsEvent.logImpression("suggested_edit_dialog")
                PatrollerExperienceEvent.logImpression("suggested_edits_dialog")

                if (ReadingChallengeWidgetRepository.shouldShowWidgetInstallDialog()) {
                    ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                        ReadingChallengeInstallWidgetDialog()
                    )
                }
            }
            binding.mainToolbarWordmark.visibility = View.GONE
            binding.mainToolbar.setTitle(tab.text)
            binding.mainToolbar.isVisible = true
            controlNavTabInFragment = true

            applyNavBarTheme(WikipediaApp.instance.currentTheme)
        }
        applyInsets()
    }

    private fun applyInsets() {
        binding.root.updatePadding(
            top = if (fragment.currentFragment is HomeFragment) 0 else statusBarInsets.top + navBarInsets.top,
            left = statusBarInsets.left + navBarInsets.left,
            right = statusBarInsets.right + navBarInsets.right
        )
        fragment.binding.mainNavTabContainer.updatePadding(
            bottom = statusBarInsets.bottom + navBarInsets.bottom
        )
    }

    private fun applyNavBarTheme(theme: Theme) {
        val wrapper = ContextThemeWrapper(this, theme.resourceId)
        val paperColor = ResourceUtil.getThemedColor(wrapper, R.attr.paper_color)
        val borderColor = ResourceUtil.getThemedColor(wrapper, R.attr.border_color)
        val colorStateList = AppCompatResources.getColorStateList(wrapper, R.color.color_state_nav_tab)
        fragment.binding.mainNavTabLayout.applyColors(paperColor, colorStateList)
        fragment.binding.mainNavTabBorder.setBackgroundColor(borderColor)
        fragment.binding.mainNavTabContainer.setBackgroundColor(paperColor)
        setNavigationBarColor(paperColor)
        DeviceUtil.setLightSystemUiVisibility(this@MainActivity, light = !theme.isDark)
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
        if (intent.hasExtra(ReadingChallengeWidgetRepository.INTENT_EXTRA_READING_CHALLENGE_JOIN)) {
            if (ReadingChallengeWidgetRepository.shouldShowOnboardingDialog()) {
                showReadingChallenge()
            }
        }
        fragment.handleIntent(intent)
    }

    override fun onGoOffline() {
        fragment.onGoOffline()
    }

    override fun onGoOnline() {
        fragment.onGoOnline()
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true) {
            TestKitchenAdapter.client.getInstrument("apps-open")
                .submitInteraction(action = "app_open", actionSource = "app_icon")
        }
        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            // TODO: handle special cases of non-article content, e.g. shared reading lists.
            intent.data?.let {
                if (it.authority.orEmpty().endsWith(WikiSite.BASE_DOMAIN)) {
                    // Pass it right along to PageActivity
                    val uri = it.toString().replace("wikipedia://", WikiSite.DEFAULT_SCHEME + "://").toUri()
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
        (fragment.currentFragment as? HomeFragment)?.refreshNotification()
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
