package org.wikipedia.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.ActivityMainBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.navtab.NavTab
import org.wikipedia.onboarding.InitialOnboardingActivity
import org.wikipedia.page.PageActivity
import org.wikipedia.places.PlacesFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.SurveyDialog

class MainActivity : SingleFragmentActivity<MainFragment>(), MainFragment.Callback {

    private lateinit var binding: ActivityMainBinding

    private var controlNavTabInFragment = false
    private val onboardingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val inAppUpdatesLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode != RESULT_OK) {
            FeedbackUtil.showMessage(this, "Something went wrong!")
        } else {
            FeedbackUtil.showMessage(this, "Downloading the APK...")
        }
    }

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateListener: InstallStateUpdatedListener

    override fun inflateAndSetContentView() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setImageZoomHelper()
        if (Prefs.isInitialOnboardingEnabled && savedInstanceState == null && !intent.hasExtra(
                Constants.INTENT_EXTRA_PREVIEW_SAVED_READING_LISTS)) {
            // Updating preference so the search multilingual tooltip
            // is not shown again for first time users
            Prefs.isMultilingualSearchTooltipShown = false

            // Use startActivityForResult to avoid preload the Feed contents before finishing the initial onboarding.
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

        appUpdateManager = AppUpdateManagerFactory.create(this)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
                    inAppUpdatesLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
            }
        }

        appUpdateListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                binding.inAppUpdateProgressBar.isVisible = true
            } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                val snackbar = FeedbackUtil.makeSnackbar(this, "An update has just been downloaded. ", Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction("RESTART") {
                    appUpdateManager.completeUpdate()
                }
                snackbar.show()
                binding.inAppUpdateProgressBar.isVisible = false
            }
        }
        appUpdateManager.registerListener(appUpdateListener)
    }

    override fun onResume() {
        maybeShowPlacesSurvey()
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(appUpdateListener)
    }

    private fun maybeShowPlacesSurvey() {
        binding.root.postDelayed({
            if (!isDestroyed && Prefs.shouldShowOneTimePlacesSurvey == PlacesFragment.SURVEY_SHOW) {
                Prefs.shouldShowOneTimePlacesSurvey = PlacesFragment.SURVEY_DO_NOT_SHOW
                SurveyDialog.showFeedbackOptionsDialog(this, Constants.InvokeSource.PLACES)
            }
        }, 1000)
    }

    override fun createFragment(): MainFragment {
        return MainFragment.newInstance()
    }

    override fun onTabChanged(tab: NavTab) {
        if (tab == NavTab.EDITS) {
            ImageRecommendationsEvent.logImpression("suggested_edit_dialog")
            PatrollerExperienceEvent.logImpression("suggested_edits_dialog")
        }
        if (tab == NavTab.EXPLORE) {
            binding.mainToolbarWordmark.visibility = View.VISIBLE
            binding.mainToolbar.title = ""
            controlNavTabInFragment = false
        } else {
            if (tab == NavTab.SEARCH && Prefs.showSearchTabTooltip) {
                FeedbackUtil.showTooltip(this, fragment.binding.mainNavTabLayout.findViewById(NavTab.SEARCH.id), getString(R.string.search_tab_tooltip), aboveOrBelow = true, autoDismiss = false)
                Prefs.showSearchTabTooltip = false
            }
            binding.mainToolbarWordmark.visibility = View.GONE
            binding.mainToolbar.setTitle(tab.text)
            controlNavTabInFragment = true
        }
        fragment.requestUpdateToolbarElevation()
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

    override fun onBackPressed() {
        if (fragment.onBackPressed()) {
            return
        }
        super.onBackPressed()
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
