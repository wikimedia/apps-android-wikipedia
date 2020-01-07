package org.wikipedia.createaccount

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CreateAccountSuccessResult(val username: String) : CreateAccountResult() , Parcelable {
    @IgnoredOnParcel
    override val status: String = "PASS"
    @IgnoredOnParcel
    override val message: String = "Account created"
}
