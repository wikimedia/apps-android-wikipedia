package org.wikipedia.edit

internal class EditAbuseFilterResult(val code: String?, val info: String?, val warning: String?) : EditResult("Failure") {

    // to simply treat these as an error.
    // This case is here because, unfortunately, an admin can create an abuse filter which
    // emits an arbitrary error code over the API.
    // TODO: More properly handle the case where the AbuseFilter throws an arbitrary error.
    // Oh, and, you know, also fix the AbuseFilter API to not throw arbitrary error codes.
    val type: Int
        get() = if (code.orEmpty().startsWith("abusefilter-warning")) {
            TYPE_WARNING
        } else if (code.orEmpty().startsWith("abusefilter-disallowed")) {
            TYPE_ERROR
        } else if (info.orEmpty().startsWith("Hit AbuseFilter")) {
            // This case is here because, unfortunately, an admin can create an abuse filter which
            // emits an arbitrary error code over the API.
            // TODO: More properly handle the case where the AbuseFilter throws an arbitrary error.
            // Oh, and, you know, also fix the AbuseFilter API to not throw arbitrary error codes.
            TYPE_ERROR
        } else {
            // We have no understanding of what kind of abuse filter response we got. It's safest
            // to simply treat these as an error.
            TYPE_ERROR
        }

    companion object {
        const val TYPE_WARNING = 1
        const val TYPE_ERROR = 2
    }
}
