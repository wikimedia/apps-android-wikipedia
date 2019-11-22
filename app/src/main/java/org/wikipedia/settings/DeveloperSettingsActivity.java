package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.util.ResourceUtil;

public class DeveloperSettingsActivity extends SingleFragmentActivity<DeveloperSettingsFragment> {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color));
    }

    @Override
    public DeveloperSettingsFragment createFragment() {
        return DeveloperSettingsFragment.newInstance();
    }
}
