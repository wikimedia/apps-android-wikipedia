package org.wikipedia.page

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.children
import org.wikipedia.databinding.ViewArticleTabLayoutBinding
import org.wikipedia.page.action.PageActionTab
import org.wikipedia.views.ConfigurableTabLayout

class PageActionTabLayout constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTabLayout(context, attrs) {
    init {
        ViewArticleTabLayoutBinding.inflate(LayoutInflater.from(context), this)
        orientation = HORIZONTAL
    }

    fun setPageActionTabsCallback(pageActionTabsCallback: PageActionTab.Callback) {
        children.iterator().forEach {
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
