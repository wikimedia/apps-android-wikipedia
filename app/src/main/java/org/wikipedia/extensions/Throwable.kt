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
    map["type"] = this.javaClass.simpleName
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
        else -> {
            map["message"] = this.message.orEmpty().take(32)
        }
    }
    return map
}
