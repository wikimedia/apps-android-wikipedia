package org.wikipedia.dataclient.mwapi

class CreateAccountResponse : MwResponse() {

    private val createaccount: Result? = null

    fun status(): String? {
        return createaccount!!.status
    }

    fun user(): String? {
        return createaccount!!.username
    }

    fun message(): String? {
        return createaccount!!.message
    }

    fun hasResult(): Boolean {
        return createaccount != null
    }

    class Result {

        val status: String? = null
            get() = field.orEmpty()
        val message: String? = null
            get() = field.orEmpty()
        val username: String? = null
            get() = field.orEmpty()
    }
}
