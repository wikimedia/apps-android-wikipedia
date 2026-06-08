package org.wikipedia.extensions

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String?): T? {
    return IntentCompat.getParcelableExtra(this, key, T::class.java)
}

inline fun <reified T : Serializable> Intent.serializableExtra(key: String?): T? {
    return IntentCompat.getSerializableExtra(this, key, T::class.java)
}
