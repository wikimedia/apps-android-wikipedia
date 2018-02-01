package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.util.ResourceUtil;

abstract class PreferenceLoaderFragment extends PreferenceFragmentCompat
        implements PreferenceLoader {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        loadPreferences();
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView v = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        v.setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        return v;
    }
}
