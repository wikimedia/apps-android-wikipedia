package org.wikipedia.inappupdates

import android.content.Context
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.language.AppLanguageLookUpTable
import org.wikipedia.util.FeedbackUtil

class InAppUpdates {
    private var appUpdateManager: AppUpdateManager? = null
    private var appUpdateListener: InstallStateUpdatedListener? = null

    fun init(activity: AppCompatActivity, progressBar: ProgressBar) {

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(WikipediaApp.instance) != ConnectionResult.SUCCESS) {
            return
        }

        appUpdateManager = AppUpdateManagerFactory.create(activity)

        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                appUpdateManager?.startUpdateFlowForResult(appUpdateInfo,
                    activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                        if (it.resultCode != AppCompatActivity.RESULT_OK) {
                            FeedbackUtil.showMessage(activity, activity.getString(R.string.in_app_update_launcher_snackbar_failure))
                        } else {
                            FeedbackUtil.showMessage(activity, activity.getString(R.string.in_app_update_launcher_snackbar_download))
                            progressBar.isVisible = true
                        }
                    }, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
            }
        }

        appUpdateListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                val snackbar = FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.in_app_update_download_completed_snackbar), Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction(activity.getString(R.string.in_app_update_restart_action).uppercase()) {
                    appUpdateManager?.completeUpdate()
                }
                snackbar.show()
                progressBar.isVisible = false
            }
        }
    }

    fun registerListener() {
        appUpdateManager?.registerListener(appUpdateListener)
    }

    fun unregisterListener() {
        appUpdateManager?.unregisterListener(appUpdateListener)
    }
}
