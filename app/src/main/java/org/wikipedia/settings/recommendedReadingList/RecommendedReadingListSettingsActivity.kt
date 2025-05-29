package org.wikipedia.settings.recommendedReadingList

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
import org.wikipedia.readinglist.recommended.RecommendedReadingListSource
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil

class RecommendedReadingListSettingsActivity : BaseActivity(), BaseActivity.Callback {

    private val viewModel: RecommendedReadingListSettingsViewModel by viewModels()
    private lateinit var currentRecommendedReadingListSource: RecommendedReadingListSource

    private val recommendedReadingListSourceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //  @TODO: call this code when discover screen is complete
        if (it.resultCode == RESULT_OK) {
            viewModel.updateRecommendedReadingListSource(Prefs.recommendedReadingListSource)
            showSnackBar(Prefs.recommendedReadingListSource, onAction = {
                viewModel.updateRecommendedReadingListSource(currentRecommendedReadingListSource)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        // @TODO: implement when discover screen is complete
                    },
                    onInterestClick = {
                        // @TODO: implement when interest screen is complete
                    },
                    onNotificationStateChanged = {
                        viewModel.toggleNotification(it)
                        // @TODO: implement actual notification after notification ticket is merged
                    },
                    onUpdateFrequency = {
                        viewModel.updateFrequency(it)
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

    override fun onPermissionResult(activity: BaseActivity, isGranted: Boolean) {}

    private fun requestPermissionAndScheduleGameNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                   // @TODO: implement after notification ticket is merged
                }
                else -> requestPermissionLauncher.launch(permission)
            }
        } else {
            // @TODO: implement after notification ticket is merged
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
}
