package org.wikipedia.activitytab

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.get
import androidx.core.view.size

class ActivityTabOverflowMenu(
    private val context: Context,
    private val anchorView: View
) {

    fun show(menuRes: Int, onMenuItemClick: (MenuItem) -> Boolean) {
        val popup = PopupMenu(context, anchorView)
        popup.menuInflater.inflate(menuRes, popup.menu)
        val menu = popup.menu
        for (i in 0 until menu.size) {
            insertMenuItemIcon(menu[i])
        }
        popup.setOnMenuItemClickListener(onMenuItemClick)
        popup.show()
    }

    /**
     * Converts the MenuItem's title into a Spannable containing both icon and title
     */
    private fun insertMenuItemIcon(menuItem: MenuItem) {
        var icon = menuItem.icon
        if (icon == null) {
            icon = TRANSPARENT.toDrawable()
        }
        val iconSize = (24 * context.resources.displayMetrics.density).toInt()
        icon.setBounds(0, 0, iconSize, iconSize)
        val imageSpan = ImageSpan(icon)

        // Add spaces for the icon, then the title
        val ssb = SpannableStringBuilder("    ${menuItem.title}")

        // Replace the first space with the icon
        ssb.setSpan(imageSpan, 0, 1, 0)

        // Update the menu item
        menuItem.title = ssb
        menuItem.icon = null // Remove original icon
    }
}
