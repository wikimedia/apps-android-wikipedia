package org.wikipedia.views

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

object SimpleAlertDialog {

    fun show(
        activity: AppCompatActivity,
        @StringRes title: Int,
        @StringRes message: Int,
        @DrawableRes icon: Int? = null,
        positiveButton: Pair<String, () -> Unit>? = null,
        negativeButton: String? = null,
        @StyleRes themeResId: Int = com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered,
        delayMillis: Long = 0,
        isCancellable: Boolean = false
    ) {

        activity.lifecycleScope.launch {
            val dialog = MaterialAlertDialogBuilder(activity, themeResId)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(isCancellable)

            if (icon != null) {
                dialog.setIcon(icon)
            }

            positiveButton?.let { (text, action) ->
                dialog.setPositiveButton(text) { _, _ ->
                    // @TODO: Take user to google form
                    action()
                }
            }

            negativeButton?.let { text ->
                dialog.setNegativeButton(text) { dialog, _ ->
                    dialog.dismiss()
                }
            }

            dialog.create()

            delay(delayMillis)
            withContext(Dispatchers.Main) {
                dialog.show()
            }
        }
    }
}