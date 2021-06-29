package org.wikipedia.dataclient.mwapi

class CreateAccountResponse : MwResponse() {

    private val createaccount: Result? = null

    val status get() = createaccount?.status

    val user get() = createaccount?.username

    val message get() = createaccount?.message

    class Result {

        val status: String = ""
        val message: String = ""
        val username: String = ""
    }
}
