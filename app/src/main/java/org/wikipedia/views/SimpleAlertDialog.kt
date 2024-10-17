package org.wikipedia.views

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimpleAlertDialog(private val context: Context) {
    @StringRes
    var title: Int? = null
    @StringRes
    var message: Int? = null
    @DrawableRes
    var icon: Int? = null
    @StyleRes
    var themeResId: Int = com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert
    var delayMillis: Long = 0
    var isCancellable: Boolean = false

    private var positiveButton: Pair<String, () -> Unit>? = null
    private var negativeButton: Pair<String, () -> Unit>? = null
    private var neutralButton: Pair<String, () -> Unit>? = null

    fun positiveButton(text: String, action: () -> Unit) {
        this.positiveButton = text to action
    }

    fun negativeButton(text: String, action: () -> Unit) {
        this.negativeButton = text to action
    }

    fun neutralButton(text: String, action: () -> Unit) {
        this.neutralButton = text to action
    }

    internal fun build() = MaterialAlertDialogBuilder(context, themeResId).apply {
        title?.let { setTitle(it) }
        message?.let { setMessage(it) }
        icon?.let { setIcon(it) }
        positiveButton?.let { (text, action) ->
            setPositiveButton(text) { _, _ -> action() }
        }
        negativeButton?.let { (text, action) ->
            setNegativeButton(text) { _, _ -> action() }
        }
        neutralButton?.let { (text, action) ->
            setNeutralButton(text) { _, _ -> action() }
        }
        setCancelable(isCancellable)
    }.create()
}

fun AppCompatActivity.showSimpleAlertDialog(block: SimpleAlertDialog.() -> Unit) {
    val builder = SimpleAlertDialog(this)
    builder.block()
    lifecycleScope.launch {
        delay(builder.delayMillis)
        withContext(Dispatchers.Main) {
            builder.build().show()
        }
    }
}
