package org.wikipedia.createaccount

import android.os.Parcelable

abstract class CreateAccountResult : Parcelable {
    abstract val status: String
    abstract val message: String
}