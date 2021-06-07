package org.wikipedia.login

import org.wikipedia.dataclient.WikiSite

open class LoginResult internal constructor(val site: WikiSite,
                                            val status: String,
                                            val userName: String?,
                                            val password: String?,
                                            val message: String?,
                                            var userId: Int = 0,
                                            var groups: Set<String> = emptySet()) {

    fun pass(): Boolean {
        return STATUS_PASS == status
    }

    fun fail(): Boolean {
        return STATUS_FAIL == status
    }

    companion object {
        const val STATUS_PASS = "PASS"
        const val STATUS_FAIL = "FAIL"
        const val STATUS_UI = "UI"
    }
}
