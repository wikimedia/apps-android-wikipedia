package org.wikipedia.page

import android.content.Context
import android.util.AttributeSet
import android.view.View
import org.wikipedia.R
import org.wikipedia.page.action.PageActionTab
import org.wikipedia.views.ConfigurableTabLayout

class PageActionTabLayout constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTabLayout(context, attrs) {
    init {
        View.inflate(getContext(), R.layout.view_article_tab_layout, this)
        orientation = HORIZONTAL
    }

    fun setPageActionTabsCallback(pageActionTabsCallback: PageActionTab.Callback) {
        for (i in 0 until childCount) {
            val tab = getChildAt(i)
            if (tab.tag != null) {
                val tabPosition = Integer.valueOf((tab.tag as String))
                tab.isFocusable = true
                tab.setOnClickListener { v: View? ->
                    if (isEnabled(v!!)) {
                        PageActionTab.of(tabPosition).select(pageActionTabsCallback)
                    }
                }
            }
        }
    }
}