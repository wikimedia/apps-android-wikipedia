package org.wikipedia.page

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.forEach
import org.wikipedia.databinding.ItemQuickActionTabBinding
import org.wikipedia.page.action.PageActionTab
import org.wikipedia.page.customize.QuickActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.views.ConfigurableTabLayout

class PageActionTabLayout constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTabLayout(context, attrs) {

    val callback: QuickActionItem.Callback? = null

    init {
        orientation = HORIZONTAL
        Prefs.customizeFavoritesQuickActionsOrder.forEach {
            val view = ItemQuickActionTabBinding.inflate(LayoutInflater.from(context), this, true).root
            val item = QuickActionItem.find(it)
            view.text = context.getString(item.titleResId)
            view.setCompoundDrawablesWithIntrinsicBounds(0, item.iconResId, 0, 0)
            view.setOnClickListener { v ->
                if (isEnabled(v) && callback != null) {
                    item.select(callback)
                }
            }
            view.isFocusable = true
            addView(view)
        }
    }

    fun setPageActionTabsCallback(pageActionTabsCallback: PageActionTab.Callback) {
        forEach {
            it.tag?.let { tag ->
                val tabPosition = tag.toString().toInt()
                it.isFocusable = true
                it.setOnClickListener { v ->
                    if (isEnabled(v)) {
                        PageActionTab.of(tabPosition).select(pageActionTabsCallback)
                    }
                }
            }
        }
    }
}
