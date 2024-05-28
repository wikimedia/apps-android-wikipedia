package org.wikipedia.extensions

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun View.coroutineScope(): CoroutineScope {
    return (context as? AppCompatActivity)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main)
}
