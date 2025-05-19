package org.wikipedia.settings.discover

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme

class DiscoverSettingsActivity : BaseActivity(), BaseActivity.Callback {

    private val discoverSourceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //  @TODO: implement when discover screen is complete
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                DiscoverScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    onBackButtonClick = {
                        onBackPressed()
                    },
                    onDiscoverSourceClick = {
                        // @TODO: implement when discover screen is complete
                    },
                    onInterestClick = {
                        // @TODO: implement when interest screen is complete
                    },
                    onNotificationChange = {
                        // @TODO: implement after notification ticket is merged
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
}
