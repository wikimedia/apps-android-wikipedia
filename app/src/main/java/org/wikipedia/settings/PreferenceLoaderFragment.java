package org.wikipedia.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.CallbackFragment.Callback;
import org.wikipedia.activity.FragmentUtil;

abstract class PreferenceLoaderFragment extends PreferenceFragmentCompat
        implements PreferenceLoader, CallbackFragment<Callback> {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        loadPreferences();
    }

    @Nullable
    @Override public Callback getCallback() {
        return FragmentUtil.getCallback(this);
    }
}