package org.wikipedia.extensions

import com.hcaptcha.sdk.HCaptchaException
import org.wikipedia.createaccount.CreateAccountException
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.okhttp.HttpStatusException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.getInstrumentActionContext(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    this::class.simpleName?.let {
        map["class"] = it
    }
    // Try to obtain the correct code, based on the type of exception
    when (this) {
        is HttpStatusException -> {
            map["code"] = this.code.toString()
        }
        is SocketTimeoutException -> {
            map["code"] = "timeout"
        }
        is UnknownHostException, is SocketException -> {
            map["code"] = "network_unavailable"
        }
        is CreateAccountException -> {
            map["code"] = this.messageCode.orEmpty()
        }
        is HCaptchaException -> {
            map["code"] = this.statusCode.toString()
        }
        is MwException -> {
            map["code"] = this.error.code.orEmpty()
        }
    }
    // ...and if we couldn't get a code, then use the message itself.
    if (!map.containsKey("code") && !this.message.isNullOrEmpty()) {
        map["message"] = this.message.orEmpty().take(64)
    }
    return map
}
