package org.wikipedia.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil

class NotificationSettingsFragment : PreferenceLoaderFragment() {
    override fun loadPreferences() {
        NotificationSettingsPreferenceLoader(this).loadPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(AppCompatResources.getDrawable(requireContext(), ResourceUtil.getThemedAttributeId(requireContext(), R.attr.list_divider)))
    }

    companion object {
        fun newInstance(): NotificationSettingsFragment {
            return NotificationSettingsFragment()
        }
    }
}
