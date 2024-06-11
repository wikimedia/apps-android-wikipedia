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
                    str += ", key: $key, message: $message"
                }
                str
            }

    constructor(rsp: Response) {
        this.code = rsp.code
        url = rsp.request.url.toUri().toString()
        try {
            rsp.body?.let {
                if (it.contentType().toString().contains("json")) {
                    val body = it.string()
                    if (body.contains("\$schema")) {
                        // This is likely an error from the Event service, and should not be parsed
                        // as a Rest Service error. Just keep the whole body of the error as the
                        // exception message.
                        message = body
                    } else {
                        serviceError = RbServiceError.create(body)
                    }
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
