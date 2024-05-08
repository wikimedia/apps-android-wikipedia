package org.wikipedia.extensions

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.core.content.IntentCompat
import java.io.Serializable
import kotlin.reflect.safeCast

inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String?): T? {
    return IntentCompat.getParcelableExtra(this, name, T::class.java)
}

inline fun <reified T : Serializable> Intent.serializableExtra(name: String?): T? {
    return getSerializableExtra(this, name, T::class.java)
}

fun <T : Serializable> getSerializableExtra(intent: Intent, name: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        intent.getSerializableExtra(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        clazz.kotlin.safeCast(intent.getSerializableExtra(name))
    }
}
