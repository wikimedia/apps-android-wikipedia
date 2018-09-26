package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;

public class DescriptionEditHelpActivity extends SingleFragmentActivity<DescriptionEditHelpFragment> {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditHelpActivity.class);
    }

    @Override
    public DescriptionEditHelpFragment createFragment() {
        return DescriptionEditHelpFragment.newInstance();
    }
}
