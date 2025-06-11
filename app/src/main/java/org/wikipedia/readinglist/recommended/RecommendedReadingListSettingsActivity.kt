package org.wikipedia.readinglist.recommended

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource

class RecommendedReadingListSettingsActivity : BaseActivity(), BaseActivity.Callback {

    private val viewModel: RecommendedReadingListSettingsViewModel by viewModels()
    private lateinit var currentRecommendedReadingListSource: RecommendedReadingListSource

    private val recommendedReadingListSourceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            viewModel.updateRecommendedReadingListSource(Prefs.recommendedReadingListSource)
            val currentTitlesWithOffset = Prefs.recommendedReadingListSourceTitlesWithOffset
            if (currentRecommendedReadingListSource != Prefs.recommendedReadingListSource) {
                showSnackBar(Prefs.recommendedReadingListSource, onAction = {
                    viewModel.updateRecommendedReadingListSource(currentRecommendedReadingListSource)
                    Prefs.recommendedReadingListSourceTitlesWithOffset = currentTitlesWithOffset
                    RecommendedReadingListEvent.submit("built_undo_click", "discover_settings")
                })
                Prefs.resetRecommendedReadingList = true
                Prefs.recommendedReadingListSourceTitlesWithOffset = emptyList()
            }
        }
    }

    private val recommendedReadingListInterestsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            viewModel.updateRecommendedReadingListSource(Prefs.recommendedReadingListSource)
            Prefs.resetRecommendedReadingList = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callback = this
        enableEdgeToEdge()
        RecommendedReadingListEvent.submit("impression", "discover_settings")
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val resetUiState by viewModel.resetUiState.collectAsState()

            BaseTheme {
                RecommendedReadingListSettingsScreen(
                    uiState = uiState,
                    resetUiState = resetUiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding(),
                    onBackButtonClick = {
                        if (resetUiState !is Resource.Loading) {
                            RecommendedReadingListEvent.submit("back_click", "discover_settings")
                            viewModel.generateRecommendedReadingList()
                        }
                    },
                    onRecommendedReadingListSourceClick = {
                        RecommendedReadingListEvent.submit("built_click", "discover_settings", selected = Prefs.recommendedReadingListSource.eventString)
                        currentRecommendedReadingListSource = Prefs.recommendedReadingListSource
                        recommendedReadingListSourceLauncher.launch(RecommendedReadingListOnboardingActivity.newIntent(this, fromSettings = true))
                    },
                    onInterestClick = {
                        RecommendedReadingListEvent.submit("interests_click", "discover_settings")
                        recommendedReadingListInterestsLauncher.launch(RecommendedReadingListOnboardingActivity.newIntent(this, startFromSourceSelection = false, fromSettings = true))
                    },
                    onNotificationStateChanged = {
                        RecommendedReadingListEvent.submit(if (it) "notifs_on_click" else "notifs_off_click", "discover_settings")
                        if (it) {
                            requestPermissionAndScheduleRecommendedReadingNotification()
                        } else {
                            RecommendedReadingListNotificationManager.cancelRecommendedReadingListNotification(this)
                            viewModel.toggleNotification(false)
                        }
                    },
                    onUpdateFrequency = {
                        val frequencyForEvent = getString(it.displayStringRes)
                        RecommendedReadingListEvent.submit("update_${frequencyForEvent}_click", "discover_settings")
                        viewModel.updateFrequency(it)
                        requestPermissionAndScheduleRecommendedReadingNotification()
                    },
                    onArticleNumberChanged = {
                        RecommendedReadingListEvent.submit("article_count_click", "discover_settings", countSaved = it)
                        viewModel.updateArticleNumbers(it)
                    },
                    onRecommendedReadingListSwitchClick = {
                        RecommendedReadingListEvent.submit(if (it) "discover_on_click" else "discover_off_click", "discover_settings")
                        viewModel.toggleDiscoverReadingList(it)
                    },
                    wikiErrorClickEvents = WikiErrorClickEvents(
                        backClickListener = {
                            onBackPressed()
                        },
                        retryClickListener = {
                            viewModel.generateRecommendedReadingList()
                        }
                    ),
                    onListGenerated = {
                        onBackPressed()
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
