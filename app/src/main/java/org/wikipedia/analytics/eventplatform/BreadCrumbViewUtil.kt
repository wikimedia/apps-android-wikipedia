package org.wikipedia.analytics.eventplatform

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.feed.view.ListCardItemView
import org.wikipedia.feed.view.ListCardView

object BreadCrumbViewUtil {
    fun getLogNameForView(view: View?): String {
        var logString = ""

        if (view?.parent != null && view.parent is RecyclerView) {
            val position = (view.parent as RecyclerView).getChildViewHolder(view).layoutPosition + 1
            if (view is ListCardItemView) {
                var currentParent = view.parent
                while (currentParent !is ListCardView<*>) {
                    if (currentParent.parent != null) {
                        currentParent = currentParent.parent
                    } else {
                        return view.context?.getString(R.string.breadcrumb_view_with_position, getLogNameForView(view.parent as RecyclerView), position).orEmpty()
                    }
                }
                return view.context?.getString(R.string.breadcrumb_view_click, view.context.getString(R.string.breadcrumb_view_with_position, currentParent.javaClass.simpleName, position)).orEmpty()
            }
            logString =
                view.context?.getString(R.string.breadcrumb_view_with_position, getLogNameForView(view.parent as RecyclerView), position).orEmpty()
            return logString
        }
        logString =
            if (view?.id == null || view.id == View.NO_ID) "no-id" else view.context?.getString(R.string.breadcrumb_view_click, view.resources.getResourceEntryName(view.id)).orEmpty()
        return logString
    }
}
