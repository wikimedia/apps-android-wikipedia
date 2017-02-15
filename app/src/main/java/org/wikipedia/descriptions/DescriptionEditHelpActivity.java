package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentToolbarActivity;

public class DescriptionEditHelpActivity extends SingleFragmentToolbarActivity<DescriptionEditHelpFragment> {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DescriptionEditHelpActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWordmarkVisible(false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.description_edit_help_title);
        }
    }

    @Override
    public DescriptionEditHelpFragment createFragment() {
        return DescriptionEditHelpFragment.newInstance();
    }
}
