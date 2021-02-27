package org.wikipedia.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Pair
import org.wikipedia.settings.Prefs

object TransitionUtil {
    @JvmStatic
    fun getSharedElements(context: Context, vararg views: View): Array<Pair<View, String>> {
        val shareElements: MutableList<Pair<View, String>> = mutableListOf()

        views.forEach {
            if (it is TextView && it.text.isNotEmpty()) {
                shareElements.add(Pair(it, it.transitionName))
            }
            if (it is ImageView && it.visibility == View.VISIBLE &&
                    (it.parent as View).visibility == View.VISIBLE &&
                    !DimenUtil.isLandscape(context) &&
                    Prefs.isImageDownloadEnabled) {
                shareElements.add(Pair(it, it.transitionName))
            }
        }
        return shareElements.toTypedArray()
    }
}
