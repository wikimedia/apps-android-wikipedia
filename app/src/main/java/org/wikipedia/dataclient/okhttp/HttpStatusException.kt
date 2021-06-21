package org.wikipedia.dataclient.okhttp

import okhttp3.Response
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.dataclient.restbase.RbServiceError
import org.wikipedia.util.log.L
import java.io.IOException

class HttpStatusException : IOException {
    val code: Int
    val url: String
    var serviceError: ServiceError? = null

    override var message: String? = null
        get() =
            if (!field.isNullOrEmpty()) {
                field!!
            } else {
                var str = "Code: $code, URL: $url"
                serviceError?.run {
                    str += ", title: $title, detail: $details"
                }
                str
            }

    constructor(rsp: Response) {
        this.code = rsp.code
        url = rsp.request.url.toUri().toString()
        try {
            rsp.body?.let {
                if (it.contentType().toString().contains("json")) {
                    serviceError = RbServiceError.create(it.string())
                }
            }
        } catch (e: Exception) {
            L.e(e)
        }
    }

    constructor(code: Int, url: String, message: String?) {
        this.code = code
        this.url = url
        this.message = message
    }
}
