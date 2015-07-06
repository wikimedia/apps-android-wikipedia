package org.wikipedia.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.view.MenuItem;

import org.wikipedia.R;
import org.wikipedia.activity.ThemedActionBarActivity;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DeveloperSettingsActivity extends ThemedActionBarActivity {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());

        if (!isFragmentCreated()) {
            addFragment(createFragment());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    private static boolean defaultOnOptionsItemSelected(Activity activity, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.onBackPressed();
                return true;
            default:
                return false;
        }
    }

    private void addFragment(Fragment fragment) {
        getFragmentManager().beginTransaction().add(getContainerId(), fragment).commit();
    }

    private DeveloperSettingsFragment getFragment() {
        //noinspection unchecked
        return (DeveloperSettingsFragment) getFragmentManager().findFragmentById(getContainerId());
    }

    private DeveloperSettingsFragment createFragment() {
        return DeveloperSettingsFragment.newInstance();
    }

    private boolean isFragmentCreated() {
        return getFragment() != null;
    }

    @IdRes
    private int getContainerId() {
        return R.id.fragment_container;
    }

    @LayoutRes
    private int getLayout() {
        return R.layout.activity_single_fragment;
    }
}