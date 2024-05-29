package org.wikipedia.extensions

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

fun View.coroutineScope(coroutineContext: CoroutineContext = Dispatchers.Main): CoroutineScope {
    return (context as? AppCompatActivity)?.lifecycleScope ?: CoroutineScope(coroutineContext)
}
