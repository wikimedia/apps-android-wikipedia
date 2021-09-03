package org.wikipedia.settings

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil

class NotificationSettingsFragment : PreferenceLoaderFragment() {
    override fun loadPreferences() {
        NotificationSettingsPreferenceLoader(this).loadPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(ContextCompat.getDrawable(requireActivity(), ResourceUtil.getThemedAttributeId(requireContext(), R.attr.list_separator_drawable)))
    }

    companion object {
        fun newInstance(): NotificationSettingsFragment {
            return NotificationSettingsFragment()
        }
    }
}
