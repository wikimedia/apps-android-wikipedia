package org.wikipedia.overhaul;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class OverhaulActivity extends SingleFragmentActivity<OverhaulFragment> {
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, OverhaulActivity.class);
    }

    @Override protected OverhaulFragment createFragment() {
        return OverhaulFragment.newInstance();
    }

    @Override protected void setTheme() { }
}