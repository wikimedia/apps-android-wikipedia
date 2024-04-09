package org.wikipedia.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

object ClipboardUtil {
    fun setPlainText(context: Context, label: CharSequence? = null, text: CharSequence?) {
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
