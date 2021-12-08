package org.wikipedia.page.customize

import androidx.lifecycle.ViewModel
import org.wikipedia.R
import org.wikipedia.settings.Prefs

class CustomizeFavoritesViewModel : ViewModel() {

    private var menuOrder = mutableListOf<Int>()
    private var quickActionsOrder = mutableListOf<Int>()
    val listOfCategory = listOf(R.string.customize_favorites_category_quick_actions, R.string.customize_favorites_category_menu)
    // List that contains header, empty placeholder and actual items.
    var fullList = mutableListOf<Any>()

    init {
        menuOrder = Prefs.customizeFavoritesMenuOrder as MutableList<Int>
        quickActionsOrder = Prefs.customizeFavoritesQuickActionsOrder as MutableList<Int>
    }

}
