package org.wikipedia.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwResponse

class LoginFailedException(message: String?) : Throwable(message)
