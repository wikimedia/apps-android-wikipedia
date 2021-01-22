package org.wikipedia.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {
    @JvmStatic
    fun setPlainText(context: Context?, label: CharSequence?, text: CharSequence?) {
        val clip = ClipData.newPlainText(label, text)
        getManager(context).setPrimaryClip(clip)
    }

    private fun getManager(context: Context?): ClipboardManager {
        return context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
}
