package org.wikipedia.agesignals

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UserData.kt
data class UserData(
    val responseStatus: String,
    val userStatus: String,
    val ageLower: Int?,
    val ageUpper: Int?,
    val userId: String?,
    val mostRecentApprovalDate: String?
)

// UserAgeDataResponse.kt
object UserAgeDataResponse {
    const val COLUMN_RESPONSE_STATUS = "responseStatus"
    const val COLUMN_USER_STATUS = "userStatus"
    const val COLUMN_USER_ID = "userId"
    const val COLUMN_MOST_RECENT_APPROVAL_DATE = "mostRecentApprovalDate"
    const val COLUMN_AGE_LOWER = "ageLower"
    const val COLUMN_AGE_UPPER = "ageUpper"
}

class AmazonUserDataClient(private val context: Context) {

    companion object {
        // For testing - returns mock data
        private const val TEST_AUTHORITY = "amzn_test_appstore"

        // For production - returns real data after Jan 1, 2026
        private const val PROD_AUTHORITY = "amzn_appstore"
        private const val PROD_PATH = "/getUserAgeData"

        // Toggle this for testing vs production
        private const val USE_TEST_MODE = true
    }

    suspend fun getUserData(testOption: Int = 1): UserData? = withContext(Dispatchers.IO) {
        val authority = if (USE_TEST_MODE) TEST_AUTHORITY else PROD_AUTHORITY
        val path = if (USE_TEST_MODE) "/getUserAgeData?testOption=$testOption" else PROD_PATH
        val uri = "content://$authority$path".toUri()

        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    // Get column indices with validation
                    val responseStatusIndex = it.getColumnIndex(UserAgeDataResponse.COLUMN_RESPONSE_STATUS)
                    val userStatusIndex = it.getColumnIndex(UserAgeDataResponse.COLUMN_USER_STATUS)
                    val ageLowerIndex = it.getColumnIndex(UserAgeDataResponse.COLUMN_AGE_LOWER)
                    val ageUpperIndex = it.getColumnIndex(UserAgeDataResponse.COLUMN_AGE_UPPER)
                    val userIdIndex = it.getColumnIndex(UserAgeDataResponse.COLUMN_USER_ID)
                    val approvalDateIndex = it.getColumnIndex(UserAgeDataResponse.COLUMN_MOST_RECENT_APPROVAL_DATE)

                    UserData(
                        responseStatus = if (responseStatusIndex >= 0) {
                            it.getString(responseStatusIndex) ?: ""
                        } else "",

                        userStatus = if (userStatusIndex >= 0) {
                            it.getString(userStatusIndex) ?: ""
                        } else "",

                        ageLower = if (ageLowerIndex >= 0 && !it.isNull(ageLowerIndex)) {
                            it.getInt(ageLowerIndex)
                        } else null,

                        ageUpper = if (ageUpperIndex >= 0 && !it.isNull(ageUpperIndex)) {
                            it.getInt(ageUpperIndex)
                        } else null,

                        userId = if (userIdIndex >= 0) {
                            it.getString(userIdIndex)
                        } else null,

                        mostRecentApprovalDate = if (approvalDateIndex >= 0) {
                            it.getString(approvalDateIndex)
                        } else null
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("AmazonAgeAPI", "Error querying user data", e)
            null
        }
    }
}

class AmazonAgeSignalsTestContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "amzn_test_appstore"
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "getUserAgeData", 1)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != 1) {
            throw IllegalArgumentException("Unknown URI: $uri")
        }

        val testOption = uri.getQueryParameter("testOption")?.toIntOrNull() ?: 1

        val cursor = MatrixCursor(arrayOf(
            UserAgeDataResponse.COLUMN_RESPONSE_STATUS,
            UserAgeDataResponse.COLUMN_USER_STATUS,
            UserAgeDataResponse.COLUMN_AGE_LOWER,
            UserAgeDataResponse.COLUMN_AGE_UPPER,
            UserAgeDataResponse.COLUMN_USER_ID,
            UserAgeDataResponse.COLUMN_MOST_RECENT_APPROVAL_DATE
        ))

        cursor.addRow(getResponse(testOption))
        return cursor
    }

    private fun getResponse(option: Int): Array<Any?> {
        return when (option) {
            1 -> arrayOf("SUCCESS", "VERIFIED", 18, null, null, null) // 18+ verified
            2 -> arrayOf("SUCCESS", "UNKNOWN", null, null, null, null) // Unknown
            3 -> arrayOf("SUCCESS", "SUPERVISED", 0, 12, "testUserId123", "2023-07-01T00:00:00.008Z") // 0-12
            4 -> arrayOf("SUCCESS", "SUPERVISED", 13, 15, "testUserId456", "2023-07-01T00:00:00.008Z") // 13-15
            5 -> arrayOf("SUCCESS", "SUPERVISED", 16, 17, "testUserId789", "2023-07-01T00:00:00.008Z") // 16-17
            6 -> arrayOf("SUCCESS", "CONSENT_NOT_GRANTED", 13, 15, "testUserId999", "2023-07-01T00:00:00.008Z") // No consent
            else -> arrayOf("SUCCESS", "", null, null, null, null) // Not applicable
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Insert not supported")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException("Delete not supported")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException("Update not supported")
}
