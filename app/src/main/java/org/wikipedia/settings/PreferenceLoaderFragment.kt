package org.wikipedia.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil.getThemedColor

abstract class PreferenceLoaderFragment : PreferenceFragmentCompat(), PreferenceLoader {
    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        requireActivity().window.decorView.post {
            if (!isAdded) {
                return@post
            }
            loadPreferences()
        }
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        val v = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        v.setBackgroundColor(getThemedColor(requireContext(), R.attr.paper_color))
        return v
    }
}
