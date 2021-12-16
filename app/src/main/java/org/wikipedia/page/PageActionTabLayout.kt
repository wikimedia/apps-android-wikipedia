package org.wikipedia.page

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import org.wikipedia.databinding.ItemQuickActionsTabBinding
import org.wikipedia.page.customize.PageActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.views.ConfigurableTabLayout

class PageActionTabLayout constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTabLayout(context, attrs) {

    lateinit var callback: PageActionItem.Callback

    init {
        Prefs.customizeFavoritesQuickActionsOrder.forEach {
            val view = ItemQuickActionsTabBinding.inflate(LayoutInflater.from(context)).root
            val item = PageActionItem.find(it)
            val param = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
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

    companion object {
        const val TAG_AVAILABLE_IN_MOBILE_WEB = 100
        const val TAG_EXTERNAL_LINK = 200
    }
}
