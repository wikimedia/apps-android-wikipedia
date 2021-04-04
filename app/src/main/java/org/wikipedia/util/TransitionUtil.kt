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
        val shareElements: MutableList<Pair<View, String>> = mutableListOf()

        views.forEach {
            if (it is TextView && it.text.isNotEmpty()) {
                shareElements.add(Pair(it, it.transitionName))
            }
            if (it is ImageView && it.isVisible && (it.parent as View).isVisible &&
                    !DimenUtil.isLandscape(context) && Prefs.isImageDownloadEnabled()) {
                shareElements.add(Pair(it, it.transitionName))
            }
        }
        return shareElements.toTypedArray()
    }
}
