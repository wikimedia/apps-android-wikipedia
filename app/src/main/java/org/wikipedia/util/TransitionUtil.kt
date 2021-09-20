package org.wikipedia.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Pair
import androidx.core.view.isVisible
import org.wikipedia.settings.Prefs

object TransitionUtil {
    @JvmStatic
    fun getSharedElements(context: Context, vararg views: View): Array<Pair<View, String>> {
        return views.filter {
            (it is TextView && it.text.isNotEmpty()) ||
                    (it is ImageView && it.isVisible && (it.parent as View).isVisible &&
                            !DimenUtil.isLandscape(context) && Prefs.isImageDownloadEnabled)
        }
                .map { Pair(it, it.transitionName) }
                .toTypedArray()
    }
}
