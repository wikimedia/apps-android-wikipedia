@file:JvmName("ContextUtil")

package org.wikipedia.ktx

import android.content.Context
import org.json.JSONException
import org.wikipedia.R
import org.wikipedia.createaccount.CreateAccountException
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.login.LoginClient

class AppError(val error: String, val detail: String?)

fun Context.getAppError(throwable: Throwable): AppError {
    val inner = throwable.innermostThrowable

    return if (throwable.isNetworkError) {
        AppError(getString(R.string.error_network_error), getString(R.string.format_error_server_message,
                inner.localizedMessage))
    } else if (throwable is HttpStatusException) {
        AppError(throwable.message!!, throwable.code().toString())
    } else if (inner is LoginClient.LoginFailedException || inner is CreateAccountException || inner is MwException) {
        AppError(inner.localizedMessage!!, "")
    } else if (throwable.containsException<JSONException>()) {
        AppError(getString(R.string.error_response_malformed), inner.localizedMessage)
    } else {
        // everything else has fallen through, so just treat it as an "unknown" error
        AppError(getString(R.string.error_unknown), inner.localizedMessage)
    }
}
