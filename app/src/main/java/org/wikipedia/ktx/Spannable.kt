package org.wikipedia.ktx

import android.text.Spannable
import androidx.core.text.getSpans

/**
 * Remove all spans of the specified type.
 *
 * Note: There is an extension of the same name defined in Core KTX, but it clears all spans.
 */
inline fun <reified T : Any> Spannable.clearSpans() = getSpans<T>().forEach { removeSpan(it) }
