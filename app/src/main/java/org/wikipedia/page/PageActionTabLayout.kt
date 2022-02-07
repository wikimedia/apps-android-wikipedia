package org.wikipedia.page

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import org.wikipedia.databinding.ItemCustomizeToolbarTabBinding
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.views.ConfigurableTabLayout

class PageActionTabLayout constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTabLayout(context, attrs) {

    lateinit var callback: PageActionItem.Callback

    init {
        update()
    }

    fun update() {
        removeAllViews()
        Prefs.customizeToolbarOrder.forEach {
            val view = ItemCustomizeToolbarTabBinding.inflate(LayoutInflater.from(context)).root
            val item = PageActionItem.find(it)
            val param = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            view.id = item.hashCode()
            view.text = context.getString(item.titleResId)
            view.setCompoundDrawablesWithIntrinsicBounds(0, item.iconResId, 0, 0)
            view.setOnClickListener { v ->
                if (isEnabled(v)) {
                    item.select(callback)
                }
            }
            view.isFocusable = true
            addView(view, param)
        }
    }
}
