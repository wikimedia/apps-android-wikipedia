package org.wikipedia.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwResponse

@Serializable
class LoginResponse : MwResponse() {

    @SerialName("clientlogin")
    private val clientLogin: ClientLogin? = null

    fun toLoginResult(site: WikiSite, password: String): LoginResult? {
        return clientLogin?.toLoginResult(site, password)
    }

    @Serializable
    private class ClientLogin {

        private val status: String? = null
        private val requests: List<Request>? = null
        private val message: String? = null
        @SerialName("username")
        private val userName: String? = null

        fun toLoginResult(site: WikiSite, password: String): LoginResult {
            var userMessage = message
            if (LoginResult.STATUS_UI == status) {
                if (requests != null) {
                    for (req in requests) {
                        if (req.id.orEmpty().endsWith("TOTPAuthenticationRequest")) {
                            return LoginOAuthResult(site, status, userName, password, message)
                        } else if (req.id.orEmpty().endsWith("PasswordAuthenticationRequest")) {
                            return LoginResetPasswordResult(site, status, userName, password, message)
                        }
                    }
                }
            } else if (LoginResult.STATUS_PASS != status && LoginResult.STATUS_FAIL != status) {
                // TODO: String resource -- Looks like needed for others in this class too
                userMessage = "An unknown error occurred."
            }
            return LoginResult(site, status!!, userName, password, userMessage)
        }
    }

    @Serializable
    private class Request {
        val id: String? = null
    }
}
