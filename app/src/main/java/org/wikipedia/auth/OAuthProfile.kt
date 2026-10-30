package org.wikipedia.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class OAuthProfile {
    @SerialName("sub") val userId: Int = 0
    @SerialName("username") val userName: String = ""
    @SerialName("realname") val realName: String = ""
    @SerialName("editcount") val editCount: Int = 0
    @SerialName("email_verified") val emailVerified: Boolean = false
    @SerialName("confirmed_email") val emailConfirmed: Boolean = false
    val email: String = ""
    val blocked: Boolean = false
    val registered: String = ""
    val groups: List<String> = emptyList()
    val rights: List<String> = emptyList()
    val grants: List<String> = emptyList()
}
