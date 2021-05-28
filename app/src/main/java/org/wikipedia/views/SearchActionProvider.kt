package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.view.ActionProvider
import androidx.core.view.WindowInsetsCompat
import org.wikipedia.R
import org.wikipedia.databinding.GroupSearchBinding
import org.wikipedia.ktx.windowInsetsControllerCompat
import org.wikipedia.util.ResourceUtil.getThemedColor

class SearchActionProvider(context: Context,
                           private val searchHintString: String,
                           private val callback: Callback) : ActionProvider(context) {
    interface Callback {
        fun onQueryTextChange(s: String)
        fun onQueryTextFocusChange()
    }

    private val binding = GroupSearchBinding.inflate(LayoutInflater.from(context))

    override fun onCreateActionView(): View {
        binding.searchInput.isFocusable = true
        binding.searchInput.isIconified = false
        binding.searchInput.maxWidth = Int.MAX_VALUE
        binding.searchInput.inputType = EditorInfo.TYPE_CLASS_TEXT
        binding.searchInput.isSubmitButtonEnabled = false
        binding.searchInput.queryHint = searchHintString
        binding.searchInput.setSearchHintTextColor(getThemedColor(context, R.attr.material_theme_de_emphasised_color))
        binding.searchInput.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                binding.searchInput.setCloseButtonVisibility(s)
                callback.onQueryTextChange(s)
                return true
            }
        })
        binding.searchInput.setOnQueryTextFocusChangeListener { _: View?, isFocus: Boolean ->
            if (!isFocus) {
                callback.onQueryTextFocusChange()
            }
        }

        // remove focus line from search plate
        val searchEditPlate = binding.searchInput.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
        binding.searchInput.windowInsetsControllerCompat?.show(WindowInsetsCompat.Type.ime())
        return binding.root
    }

    override fun overridesItemVisibility(): Boolean {
        return true
    }
}
