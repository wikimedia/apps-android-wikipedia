package org.wikipedia.richtext

import android.widget.TextView
import org.wikipedia.util.StringUtil

fun TextView.setHtml(source: String?) {
    this.text = StringUtil.fromHtml(source).trim()
}
