package org.wikipedia.login

import org.wikipedia.dataclient.WikiSite

open class LoginResult constructor(val site: WikiSite,
                                   val status: String,
                                   val userName: String?,
                                   val password: String?,
                                   val message: String?,
                                   var userId: Int = 0,
                                   var groups: Set<String> = emptySet()) {

    val pass get() = STATUS_PASS == status
    val fail get() = STATUS_FAIL == status

    companion object {
        const val STATUS_PASS = "PASS"
        const val STATUS_FAIL = "FAIL"
    }
}
