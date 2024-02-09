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

    fun init(activity: AppCompatActivity, progressBar: ProgressBar) {
        // stub
    }

    fun registerListener() {
        // stub
    }

    fun unregisterListener() {
        // stub
    }
}
