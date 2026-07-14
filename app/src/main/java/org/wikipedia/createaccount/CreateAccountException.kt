package org.wikipedia.createaccount

class CreateAccountException internal constructor(message: String, val messageCode: String? = null) : RuntimeException(message)
