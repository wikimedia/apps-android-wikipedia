package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.view.ActionProvider
import butterknife.BindView
import butterknife.ButterKnife
import org.wikipedia.R
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

class SearchActionProvider(context: Context, private val searchHintString: String, private val callback: Callback) : ActionProvider(context) {

    interface Callback {
        fun onQueryTextChange(s: String?)
        fun onQueryTextFocusChange()
    }

    override fun overridesItemVisibility(): Boolean = true

    override fun onCreateActionView(): View {
        val view = View.inflate(context, R.layout.group_search, null)

        val searchView = view.findViewById<CabSearchView>(R.id.search_input)

        searchView.isFocusable = true
        searchView.requestFocusFromTouch()
        searchView.isIconified = false
        searchView.maxWidth = Int.MAX_VALUE
        searchView.inputType = EditorInfo.TYPE_CLASS_TEXT
        searchView.isSubmitButtonEnabled = false
        searchView.queryHint = searchHintString
        searchView.setSearchHintTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean = false

            override fun onQueryTextChange(s: String): Boolean {
                searchView.setCloseButtonVisibility(s)
                callback.onQueryTextChange(s)
                return true
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _: View?, isFocus: Boolean ->
            if (!isFocus) callback.onQueryTextFocusChange()
        }

        // remove focus line from search plate
        val searchEditPlate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)

        DeviceUtil.showSoftKeyboard(searchView)
        return view
    }
}
