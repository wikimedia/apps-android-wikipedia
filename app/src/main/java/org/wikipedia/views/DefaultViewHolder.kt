package org.wikipedia.views

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** The minimum boilerplate required by the view holder pattern for use with custom views.  */
open class DefaultViewHolder<T : View>(view: T) : RecyclerView.ViewHolder(view) {
    open val view: T
        get() = itemView as T
}
