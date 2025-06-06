package org.wikipedia.readinglist.recommended

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RecommendedReadingListNotificationManager
import org.wikipedia.util.FeedbackUtil

class RecommendedReadingListSettingsActivity : BaseActivity(), BaseActivity.Callback {

    private val viewModel: RecommendedReadingListSettingsViewModel by viewModels()
    private lateinit var currentRecommendedReadingListSource: RecommendedReadingListSource

    private val recommendedReadingListSourceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            viewModel.updateRecommendedReadingListSource(Prefs.recommendedReadingListSource)
            if (currentRecommendedReadingListSource != Prefs.recommendedReadingListSource) {
                showSnackBar(Prefs.recommendedReadingListSource, onAction = {
                    viewModel.updateRecommendedReadingListSource(currentRecommendedReadingListSource)
                })
                Prefs.resetRecommendedReadingList = true
            }
        }
    }

    private val recommendedReadingListInterestsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            Prefs.resetRecommendedReadingList = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callback = this
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            BaseTheme {
                RecommendedReadingListSettingsScreen(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxSize(),
                    onBackButtonClick = {
                        onBackPressed()
                    },
                    onRecommendedReadingListSourceClick = {
                        currentRecommendedReadingListSource = Prefs.recommendedReadingListSource
                        recommendedReadingListSourceLauncher.launch(RecommendedReadingListOnboardingActivity.newIntent(this, fromSettings = true))
                    },
                    onInterestClick = {
                        recommendedReadingListInterestsLauncher.launch(RecommendedReadingListOnboardingActivity.newIntent(this, startFromSourceSelection = false, fromSettings = true))
                    },
                    onNotificationStateChanged = {
                        if (it) {
                            requestPermissionAndScheduleRecommendedReadingNotification()
                        } else {
                            RecommendedReadingListNotificationManager.cancelRecommendedReadingListNotification(this)
                            viewModel.toggleNotification(false)
                        }
                    },
                    onUpdateFrequency = {
                        viewModel.updateFrequency(it)
                        requestPermissionAndScheduleRecommendedReadingNotification()
                    },
                    onArticleNumberChanged = {
                        viewModel.updateArticleNumbers(it)
                    },
                    onRecommendedReadingListSwitchClick = {
                        viewModel.toggleDiscoverReadingList(it)
                    }
                )
            }
        }
    }

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {
        if (isGranted) {
            RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(this)
            viewModel.toggleNotification(true)
        } else {
            viewModel.toggleNotification(false)
        }
    }

    private fun requestPermissionAndScheduleRecommendedReadingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                    RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(this)
                    viewModel.toggleNotification(true)
                }
                else -> requestPermissionLauncher.launch(permission)
            }
        } else {
            RecommendedReadingListNotificationManager.scheduleRecommendedReadingListNotification(this)
            viewModel.toggleNotification(true)
        }
    }

    private fun showSnackBar(recommendedReadingListSource: RecommendedReadingListSource, onAction: () -> Unit) {
        val message = getString(when (recommendedReadingListSource) {
            RecommendedReadingListSource.INTERESTS -> R.string.recommended_reading_list_settings_updates_base_snackbar_interests
            RecommendedReadingListSource.READING_LIST -> R.string.recommended_reading_list_settings_updates_base_snackbar_saved
            RecommendedReadingListSource.HISTORY -> R.string.recommended_reading_list_settings_updates_base_snackbar_history
        })
        FeedbackUtil.makeSnackbar(this, message)
            .setAction(R.string.recommended_reading_list_settings_updates_base_snackbar_action) {
                onAction()
            }
            .show()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, RecommendedReadingListSettingsActivity::class.java)
        }
    }
}
