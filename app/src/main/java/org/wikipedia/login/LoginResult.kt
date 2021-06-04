package org.wikipedia.login

import org.wikipedia.dataclient.WikiSite

open class LoginResult internal constructor(val site: WikiSite, val status: String, val userName: String?,
    val password: String?, val message: String?) {

    var userId = 0
    var groups = emptySet<String>()

    fun pass(): Boolean {
        return "PASS" == status
    }

    fun fail(): Boolean {
        return "FAIL" == status
    }
}
