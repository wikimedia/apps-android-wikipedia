package org.wikipedia.games.onthisday

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.databinding.ActivityOnThisDayGameBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.UriUtil
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class OnThisDayGameActivity : BaseActivity(), BaseActivity.Callback {

    private lateinit var binding: ActivityOnThisDayGameBinding
    private val viewModel: OnThisDayGameViewModel by viewModels()

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnThisDayGameBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        callback = this

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            val newStatusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            val newNavBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            val toolbarHeight = DimenUtil.getToolbarHeightPx(this)

            binding.appBarLayout.updatePadding(top = newStatusBarInsets.top)

            binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = toolbarHeight + newStatusBarInsets.top + newNavBarInsets.top
                leftMargin = newStatusBarInsets.left + newNavBarInsets.left
                rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            }
            windowInsets
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OnThisDayGameMenuFragment.newInstance(viewModel.invokeSource), null)
            .addToBackStack(null)
            .commit()
        hideAppBarDateText()
    }

    fun hideAppBarDateText() {
        binding.dateText.isVisible = false
    }

    fun showAppBarDateText() {
        binding.dateText.isVisible = true
    }

    fun updateAppBarDateText(text: String) {
        binding.dateText.text = text
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_on_this_day_game, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val notificationItem = menu.findItem(R.id.menu_notifications)
        val volume = menu.findItem(R.id.menu_volume)
        val volumeIcon = if (Prefs.isOtdSoundOn) R.drawable.volume_off_24dp else R.drawable.volume_up_24dp
        val volumeTitle = if (Prefs.isOtdSoundOn) getString(R.string.on_this_day_game_sound_off) else getString(R.string.on_this_day_game_sound_on)
        volume.setIcon(volumeIcon)
        volume.title = volumeTitle
        if (viewModel.gameState.value is OnThisDayGameViewModel.GameEnded) {
            notificationItem?.isVisible = true
            notificationItem?.setIcon(Prefs.otdNotificationState.getIcon())
        } else {
            notificationItem?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_learn_more -> {
                WikiGamesEvent.submit("about_click", "game_play", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)
                UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.on_this_day_game_wiki_url)))
                true
            }
            R.id.menu_report_feature -> {
                WikiGamesEvent.submit("report_click", "game_play", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)

                FeedbackUtil.composeEmail(this,
                    subject = getString(R.string.on_this_day_game_report_email_subject),
                    body = getString(R.string.on_this_day_game_report_email_body))
                true
            }
            R.id.menu_notifications -> {
                WikiGamesEvent.submit("notification_click", "game_play", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)

                OnThisDayGameNotificationManager.handleNotificationClick(this)
                true
            }
            R.id.menu_volume -> {
                Prefs.isOtdSoundOn = !Prefs.isOtdSoundOn
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
        }
    }

    override fun onBackPressed() {
        WikiGamesEvent.submit("exit_click", "game_play", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)
        if (viewModel.gameState.value !is Resource.Loading &&
            !isGameMenuFragmentVisible() &&
            viewModel.gameState.value !is OnThisDayGameViewModel.GameEnded) {
            showPauseDialog()
            return
        }

        super.onBackPressed()
        onFinish()
    }

    private fun isGameMenuFragmentVisible(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) is OnThisDayGameMenuFragment
    }

    private fun onFinish() {
        if (viewModel.invokeSource == Constants.InvokeSource.NOTIFICATION) {
            goToMainTab()
        } else {
            finish()
        }
    }

    private fun goToMainTab() {
        startActivity(MainActivity.newIntent(this)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
            .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()))
        finish()
    }

    private fun showPauseDialog() {
        WikiGamesEvent.submit("impression", "pause_modal", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme_Icon)
            .setIcon(R.drawable.ic_pause_filled_24)
            .setTitle(R.string.on_this_day_game_pause_title)
            .setMessage(R.string.on_this_day_game_pause_body)
            .setPositiveButton(R.string.on_this_day_game_pause_positive) { _, _ ->
                WikiGamesEvent.submit("pause_click", "pause_modal", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)
                finish()
            }
            .setNegativeButton(R.string.on_this_day_game_pause_negative) { _, _ ->
                WikiGamesEvent.submit("cancel_click", "pause_modal", slideName = viewModel.getCurrentScreenName(), isArchive = viewModel.isArchiveGame)
            }
            .show()
    }

    fun requestPermissionAndScheduleGameNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
                }
                else -> requestPermissionLauncher.launch(permission)
            }
        } else {
            OnThisDayGameNotificationManager.scheduleDailyGameNotification(this)
        }
    }

    companion object {
        fun newIntent(context: Context, invokeSource: Constants.InvokeSource, wikiSite: WikiSite): Intent {
            val intent = Intent(context, OnThisDayGameActivity::class.java)
                .putExtra(Constants.ARG_WIKISITE, wikiSite)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
            if (Prefs.lastOtdGameDateOverride.isNotEmpty()) {
                val date = try {
                    LocalDate.parse(Prefs.lastOtdGameDateOverride, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (_: Exception) {
                    LocalDate.now()
                }
                intent.putExtra(OnThisDayGameViewModel.EXTRA_DATE, date.atStartOfDay().toInstant(ZoneOffset.UTC).epochSecond)
            }
            return intent
        }
    }
}
