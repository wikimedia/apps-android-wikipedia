package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ActionProvider.VisibilityListener
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.view.ActionProvider
import org.wikipedia.R
import org.wikipedia.databinding.ViewSearchAndFilterBinding
import org.wikipedia.notifications.NotificationsFiltersActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

class SearchAndFilterActionProvider(context: Context,
                                    private val searchHintString: String,
                                    private val callback: Callback) : ActionProvider(context) {
    interface Callback {
        fun onQueryTextChange(s: String)
        fun onQueryTextFocusChange()
    }

    private val binding = ViewSearchAndFilterBinding.inflate(LayoutInflater.from(context))

    override fun onCreateActionView(): View {
        binding.searchInput.isFocusable = true
        binding.searchInput.isIconified = false
        binding.searchInput.maxWidth = Int.MAX_VALUE
        binding.searchInput.inputType = EditorInfo.TYPE_CLASS_TEXT
        binding.searchInput.isSubmitButtonEnabled = false
        binding.searchInput.queryHint = searchHintString
        binding.searchInput.setSearchHintTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color))
        updateFilterIconAndText()
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
        binding.notificationFilterIcon.setOnClickListener {
            context.startActivity(NotificationsFiltersActivity.newIntent(context))
        }

        // remove focus line from search plate
        val searchEditPlate = binding.searchInput.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
        DeviceUtil.showSoftKeyboard(binding.searchInput)
        return binding.root
    }

    override fun overridesItemVisibility(): Boolean {
        return true
    }

    fun updateFilterIconAndText() {
        val delimitedFilterString = Prefs.getNotificationsFilterLanguageCodes().orEmpty().split(",").filter { it.isNotEmpty() }.size.toString()
        binding.notificationFilterCount.text = delimitedFilterString
        if (delimitedFilterString == "0") {
            binding.notificationFilterCount.visibility = View.GONE
            binding.notificationFilterIcon.imageTintList =
                ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.chip_text_color))
        } else {
            binding.notificationFilterCount.visibility = View.VISIBLE
            binding.notificationFilterIcon.imageTintList =
                ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
        }
    }

}
