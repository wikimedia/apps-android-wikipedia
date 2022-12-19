package org.wikipedia.analytics.eventplatform

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.view.ListCardView

object BreadCrumbCustomLogHelper {

    fun getCustomNameForListItemView(view: View, position: Int): String {
        var currentParent = view.parent
        while (currentParent !is ListCardView<*>) {
            if (currentParent.parent != null) {
                currentParent = currentParent.parent
            } else {
                // ListItemView is not in a CardView
                return BreadCrumbViewUtil.getReadableNameForView(view.parent as RecyclerView) + "." + position
            }
        }
        return currentParent.javaClass.simpleName + "." + position
    }

    fun getCustomNameForLanguageListItem(view: View): String {
        return WikipediaApp.instance.languageState.appLanguageCodes.toString() + "language_added:" +
                view.findViewById<TextView>(R.id.language_subtitle).text.toString()
    }
}
