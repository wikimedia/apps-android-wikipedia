package org.wikipedia.agesignals

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SamsungAgeSignalData(
    val userStatus: String,
    val ageLower: String?,
    val ageUpper: String?,
    val mostRecentApprovalDate: String?,
    val installId: String?
)

sealed class SamsungAgeSignalResult {
    data class Success(val data: SamsungAgeSignalData) : SamsungAgeSignalResult()
    data class Failure(val message: String) : SamsungAgeSignalResult()
    object ProviderNotAvailable : SamsungAgeSignalResult()
}

class SamsungAgeSignalsClient(private val context: Context) {

    companion object {
        private const val TAG = "SamsungAgeSignals"

        // Samsung Galaxy Store package name
        private const val GALAXY_STORE = "com.sec.android.app.samsungapps"

        // ContentProvider authority
        private const val ASAA_AUTHORITY = "com.sec.android.app.samsungapps.provider.ASAA"

        // Metadata key for version check
        private const val ASAA_META = "$GALAXY_STORE.AccountabilityActProvider.version"

        // URI for settings
        private const val QUERY_SETTINGS = "settings"
        private const val URI_ASAA_SETTINGS = "content://$ASAA_AUTHORITY/$QUERY_SETTINGS"

        // Method name to call
        private const val METHOD_GET_AGE_SIGNAL_RESULT = "getAgeSignalResult"

        // Result bundle keys
        private const val KEY_RESULT_CODE = "result_code"
        private const val KEY_RESULT_MESSAGE = "result_message"
        private const val KEY_RESULT_USER_STATUS = "userStatus"
        private const val KEY_RESULT_AGE_LOWER = "ageLower"
        private const val KEY_RESULT_AGE_UPPER = "ageUpper"
        private const val KEY_RESULT_APPROVAL_DATE = "mostRecentApprovalDate"
        private const val KEY_RESULT_INSTALL_ID = "installID"

        // Minimum required version
        private const val MIN_PROVIDER_VERSION = 1.0f
    }

    /**
     * Check if Galaxy Store supports the Age Signals API
     * Requires Galaxy Store version 4.6.03.1 or higher
     */
    private fun checkProviderAvailable(): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                GALAXY_STORE,
                PackageManager.GET_META_DATA
            )

            val version = appInfo.metaData?.getFloat(ASAA_META, 0f) ?: 0f
            val isSupported = version >= MIN_PROVIDER_VERSION

            Log.d(TAG, "Galaxy Store provider version: $version, supported: $isSupported")
            isSupported
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Galaxy Store not installed", e)
            false
        }
    }

    /**
     * Get age signals from Samsung Galaxy Store
     * This uses ContentResolver.call() instead of query()
     */
    suspend fun getAgeSignals(): SamsungAgeSignalResult = withContext(Dispatchers.IO) {
        // Step 1: Check if provider is available
        if (!checkProviderAvailable()) {
            Log.w(TAG, "Galaxy Store provider not available")
            return@withContext SamsungAgeSignalResult.ProviderNotAvailable
        }

        try {
            // Step 2: Make the call to Samsung's ContentProvider
            val uri = URI_ASAA_SETTINGS.toUri()
            val resultBundle = context.contentResolver.call(uri, METHOD_GET_AGE_SIGNAL_RESULT, null, null)

            // Step 3: Parse the response
            if (resultBundle != null) {
                val resultCode = resultBundle.getInt(KEY_RESULT_CODE, 1)

                if (resultCode == 0) {
                    // Success - extract age signal data
                    val userStatus = resultBundle.getString(KEY_RESULT_USER_STATUS, "")
                    val ageLower = resultBundle.getString(KEY_RESULT_AGE_LOWER)
                    val ageUpper = resultBundle.getString(KEY_RESULT_AGE_UPPER)
                    val approvalDate = resultBundle.getString(KEY_RESULT_APPROVAL_DATE)
                    val installId = resultBundle.getString(KEY_RESULT_INSTALL_ID)

                    SamsungAgeSignalResult.Success(
                        SamsungAgeSignalData(
                            userStatus = userStatus,
                            ageLower = ageLower,
                            ageUpper = ageUpper,
                            mostRecentApprovalDate = approvalDate,
                            installId = installId
                        )
                    )
                } else {
                    // Failure - get error message
                    val errorMessage = resultBundle.getString(KEY_RESULT_MESSAGE, "Unknown error")
                    Log.e(TAG, "Failure: $errorMessage")
                    SamsungAgeSignalResult.Failure(errorMessage)
                }
            } else {
                Log.e(TAG, "Result bundle is null")
                SamsungAgeSignalResult.Failure("No response from Galaxy Store")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            SamsungAgeSignalResult.Failure(e.message ?: "Unknown error")
        }
    }
}
