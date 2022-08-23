package org.wikipedia

import android.net.Uri
import androidx.core.content.FileProvider

/**
 * According to the documentation:
 * "It is possible to use FileProvider directly instead of extending it. However, this is not
 * reliable and will causes (sic) crashes on some devices."
 * https://developer.android.com/reference/androidx/core/content/FileProvider
 */
class WikipediaFileProvider : FileProvider(R.xml.file_paths) {
    override fun getType(uri: Uri): String? {
        return if (uri.toString().endsWith(".wikipedia")) WIKIPEDIA_MIME_TYPE else super.getType(uri)
    }

    companion object {
        const val WIKIPEDIA_MIME_TYPE = "application/json"
    }
}
