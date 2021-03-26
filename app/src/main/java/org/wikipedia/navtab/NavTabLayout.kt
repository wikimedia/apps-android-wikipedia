package org.wikipedia.navtab

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Menu
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.core.view.isNotEmpty
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.wikipedia.R

class NavTabLayout constructor(context: Context, attrs: AttributeSet) : BottomNavigationView(context, attrs) {
    init {
        menu.clear()
        for (i in 0 until NavTab.size()) {
            val navTab = NavTab.of(i)
            menu.add(Menu.NONE, navTab.id(), i, navTab.text()).setIcon(navTab.icon())
        }
        fixTextStyle()
    }

    private fun fixTextStyle() {
        if (isNotEmpty()) {
            (getChildAt(0) as ViewGroup).forEach {
                val largeLabel = it.findViewById<TextView>(R.id.largeLabel)
                largeLabel.ellipsize = TextUtils.TruncateAt.END
                largeLabel.updatePadding(left = 0, right = 0)
                val smallLabel = it.findViewById<TextView>(R.id.smallLabel)
                smallLabel.ellipsize = TextUtils.TruncateAt.END
                smallLabel.updatePadding(left = 0, right = 0)
            }
        }
    }
}
